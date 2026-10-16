package me.friendly.exeter.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import me.friendly.exeter.render.ModelWireframe;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class PopChams extends ToggleableModule {
  private final NumberProperty<Double> riseDistance =
      new NumberProperty<Double>(2.0, -5.0, 5.0, "Rise Distance");
  private final NumberProperty<Double> fadeTime =
      new NumberProperty<Double>(1.5, 0.5, 10.0, "Fade Time");
  private final NumberProperty<Float> spin = new NumberProperty<Float>(180f, 0f, 720f, "Spin");
  private final Property<Boolean> fade = new Property<Boolean>(true, "Fade", "fade");
  private final Property<Boolean> fill = new Property<Boolean>(false, "Fill", "fill");
  private final NumberProperty<Float> lineWidth =
      new NumberProperty<Float>(2.0f, 0.5f, 5.0f, "Line Width");
  private final NumberProperty<Double> range =
      new NumberProperty<Double>(64.0, 8.0, 256.0, "Range");
  private final Property<Boolean> self = new Property<Boolean>(false, "Self", "self");
  private final Property<Boolean> thirdPerson =
      new Property<Boolean>(true, "Third Person", "thirdPerson");
  private final Property<Boolean> useCustomAlpha =
      new Property<Boolean>(false, "Custom Alpha", "CustomAlpha");
  private final NumberProperty<Float> fillAlpha =
      new NumberProperty<Float>(60f, 0f, 255f, "Fill Alpha", "FillAlpha");
  private final NumberProperty<Float> outlineAlpha =
      new NumberProperty<Float>(255f, 0f, 255f, "Outline Alpha", "OutlineAlpha");

  private final Set<Integer> pending = ConcurrentHashMap.newKeySet();
  private final Queue<Pop> pops = new ConcurrentLinkedQueue<>();
  private int activePops;

  public PopChams() {
    super("PopChams", new String[] {"popchams", "pop-chams"}, 0xFFAA00, ModuleType.RENDER);
    setDescription(
        "Freezes a player's model wireframe where they pop a totem, then it rises and fades.");
    fillAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    outlineAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    thirdPerson.visibleWhen(() -> self.getValue());
    offerProperties(
        riseDistance,
        fadeTime,
        spin,
        fade,
        fill,
        lineWidth,
        range,
        self,
        thirdPerson,
        useCustomAlpha,
        fillAlpha,
        outlineAlpha);
    riseDistance.setDescription("How far the wireframe rises. 0 stays put, negative sinks it.");
    fadeTime.setDescription("How long each pop lasts, in seconds. Rise always spans this time.");
    spin.setDescription("Wireframe spin speed in degrees per second. 0 disables spin.");
    fade.setDescription("Fade the wireframe out over the fade time.");
    fill.setDescription("Also fill the model faces. Only applies while Spin is 0.");
    lineWidth.setDescription("Wireframe line thickness in pixels.");
    range.setDescription("How far from you pops are rendered.");
    self.setDescription("Render pops for your own totem pops.");
    thirdPerson.setDescription("Only render your own pops while in third person camera.");
    useCustomAlpha.setDescription(
        "Use the Fill/Outline Alpha below instead of the global ESP alphas.");
    fillAlpha.setDescription("Fill opacity, 0-255.");
    outlineAlpha.setDescription("Wireframe opacity, 0-255.");

    this.listeners.add(
        new Listener<PacketEvent>("pop_chams_packet") {
          @Override
          public void call(PacketEvent event) {
            onPacket(event);
          }
        });
    this.listeners.add(
        new Listener<WorldRenderEvent>("pop_chams_render") {
          @Override
          public void call(WorldRenderEvent event) {
            onRender();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    DebugLogger.get()
        .log(
            getLabel(),
            DebugLogger.Level.INFO,
            "enabled, rise " + riseDistance.getValue() + " over " + fadeTime.getValue() + "s");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    pops.clear();
    pending.clear();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  @Override
  public String getTag() {
    return activePops > 0 ? String.valueOf(activePops) : null;
  }

  private void onPacket(PacketEvent event) {
    if (minecraft.level == null || minecraft.player == null) return;
    if (!(event.getPacket() instanceof ClientboundEntityEventPacket packet)) return;
    if (packet.getEventId() != 35) return;

    var entity = packet.getEntity(minecraft.level);
    if (!(entity instanceof Player player)) return;
    if (player == minecraft.player) {
      if (!self.getValue()) return;
      if (thirdPerson.getValue() && minecraft.options.getCameraType().isFirstPerson()) return;
    }

    pending.add(player.getId());
    DebugLogger.get()
        .logFile(
            getLabel(),
            player.getName().getString()
                + " popped at "
                + String.format("%.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ()));
  }

  private void onRender() {
    if (minecraft.level == null || minecraft.player == null) return;

    float partial = ModelWireframe.currentPartialTick();
    if (!pending.isEmpty()) {
      for (Player player : minecraft.level.players()) {
        if (pending.remove(player.getId())) {
          List<ModelWireframe.Quad> quads = ModelWireframe.capture(player, partial);
          if (!quads.isEmpty()) {
            while (pops.size() >= 32) pops.poll();
            pops.add(new Pop(quads, System.currentTimeMillis()));
          }
        }
      }
      pending.clear();
    }

    double dist = riseDistance.getValue();
    double total = Math.max(0.1, fadeTime.getValue());
    long now = System.currentTimeMillis();
    float lw = lineWidth.getValue();
    float spinDeg = spin.getValue();
    boolean filled = fill.getValue() && spinDeg == 0f;
    double rangeSq = range.getValue() * range.getValue();

    int count = 0;
    for (Pop pop : pops) {
      double elapsed = (now - pop.startMs) / 1000.0;
      double progress = elapsed / total;
      if (progress >= 1.0) {
        pops.remove(pop);
        continue;
      }
      count++;

      double dx = pop.cx - minecraft.player.getX();
      double dz = pop.cz - minecraft.player.getZ();
      if (dx * dx + dz * dz > rangeSq) continue;

      float fadeScale = fade.getValue() ? (float) (1.0 - progress) : 1.0f;
      int fillColor =
          ARGB.color(Math.round(baseFillAlpha() * fadeScale), EspRenderManager.getClientColor());
      int outlineColor =
          ARGB.color(Math.round(baseOutlineAlpha() * fadeScale), EspRenderManager.getClientColor());

      double yOff = dist * progress;
      double angleRad = spinDeg == 0f ? 0.0 : Math.toRadians(spinDeg * (float) elapsed);
      double cos = Math.cos(angleRad);
      double sin = Math.sin(angleRad);

      for (ModelWireframe.Quad quad : pop.quads) {
        Vec3 a = place(quad.a(), pop, yOff, cos, sin);
        Vec3 b = place(quad.b(), pop, yOff, cos, sin);
        Vec3 c = place(quad.c(), pop, yOff, cos, sin);
        Vec3 d = place(quad.d(), pop, yOff, cos, sin);
        if (filled) {
          Gizmos.rect(a, b, c, d, GizmoStyle.fill(fillColor)).setAlwaysOnTop();
        }
        Gizmos.line(a, b, outlineColor, lw).setAlwaysOnTop();
        Gizmos.line(b, c, outlineColor, lw).setAlwaysOnTop();
        Gizmos.line(c, d, outlineColor, lw).setAlwaysOnTop();
        Gizmos.line(d, a, outlineColor, lw).setAlwaysOnTop();
      }
    }
    activePops = count;
  }

  private Vec3 place(Vec3 v, Pop pop, double yOff, double cos, double sin) {
    double dx = v.x - pop.cx;
    double dz = v.z - pop.cz;
    return new Vec3(pop.cx + dx * cos - dz * sin, v.y + yOff, pop.cz + dx * sin + dz * cos);
  }

  private int baseFillAlpha() {
    return useCustomAlpha.getValue()
        ? Math.round(fillAlpha.getValue())
        : EspRenderManager.getGlobalFillAlpha();
  }

  private int baseOutlineAlpha() {
    return useCustomAlpha.getValue()
        ? Math.round(outlineAlpha.getValue())
        : EspRenderManager.getGlobalOutlineAlpha();
  }

  private static final class Pop {
    final List<ModelWireframe.Quad> quads;
    final double cx;
    final double cz;
    final long startMs;

    Pop(List<ModelWireframe.Quad> quads, long startMs) {
      this.quads = new ArrayList<>(quads);
      double sx = 0;
      double sz = 0;
      int n = 0;
      for (ModelWireframe.Quad q : quads) {
        for (Vec3 v : new Vec3[] {q.a(), q.b(), q.c(), q.d()}) {
          sx += v.x;
          sz += v.z;
          n++;
        }
      }
      this.cx = sx / n;
      this.cz = sz / n;
      this.startMs = startMs;
    }
  }
}
