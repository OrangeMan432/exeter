package me.friendly.exeter.module.impl.toggle.render;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Break highlight ported from Lemon's BreakHighlight: boxes blocks other players are mining
 * (tracked from server break-anim packets) with a name and progress tag.
 */
public class BreakHighlight extends ToggleableModule {

  public enum RenderMode {
    OUTLINED,
    FILLED,
    BOTH
  }

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(64.0, 0.0, 256.0, "Range");
  private final NumberProperty<Double> playerRange =
      new NumberProperty<Double>(16.0, 0.0, 64.0, "Player Range");
  private final Property<Boolean> showProgress = new Property<Boolean>(true, "Show Progress");
  private final EnumProperty<RenderMode> renderMode =
      new EnumProperty<RenderMode>(RenderMode.BOTH, "Render", "mode");
  private final NumberProperty<Float> lineWidth =
      new NumberProperty<Float>(2.0f, 0.5f, 5.0f, "Width");
  private final Property<Boolean> useCustomAlpha =
      new Property<Boolean>(false, "Custom Alpha", "CustomAlpha");
  private final NumberProperty<Float> fillAlpha =
      new NumberProperty<Float>(100f, 0f, 255f, "Fill Alpha", "FillAlpha");
  private final NumberProperty<Float> outlineAlpha =
      new NumberProperty<Float>(255f, 0f, 255f, "Outline Alpha", "OutlineAlpha");
  private final NumberProperty<Float> textScale =
      new NumberProperty<Float>(0.4f, 0.25f, 4.0f, "Text Scale");

  private static final class TrackedBreak {
    final BlockPos pos;
    int stage;
    final long start;
    float shown;
    long lastUpdate;

    TrackedBreak(BlockPos pos, int stage) {
      this.pos = pos;
      this.stage = stage;
      this.start = System.currentTimeMillis();
      this.shown = 0.0f;
      this.lastUpdate = this.start;
    }
  }

  private final Map<UUID, TrackedBreak> breaking = new HashMap<>();

  public BreakHighlight() {
    super(
        "BreakHighlight", new String[] {"breakhighlight", "breakhi"}, 0x55FF55, ModuleType.RENDER);
    setDescription("Boxes blocks other players are mining, with progress.");
    fillAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    outlineAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    offerProperties(
        range,
        playerRange,
        showProgress,
        renderMode,
        lineWidth,
        useCustomAlpha,
        fillAlpha,
        outlineAlpha,
        textScale);
    this.listeners.add(
        new Listener<PacketEvent>("break_highlight_packet") {
          @Override
          public void call(PacketEvent event) {
            if (event.getPacket() instanceof ClientboundBlockDestructionPacket packet) {
              onBreakAnim(packet);
            } else if (event.isSending()
                && event.getPacket() instanceof ServerboundPlayerActionPacket digPacket) {
              onSelfDig(digPacket);
            }
          }
        });
    this.listeners.add(
        new Listener<WorldRenderEvent>("break_highlight_render") {
          @Override
          public void call(WorldRenderEvent event) {
            onRender();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    breaking.clear();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    breaking.clear();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  private void onBreakAnim(ClientboundBlockDestructionPacket packet) {
    if (minecraft.level == null || minecraft.player == null) return;
    Entity entity = minecraft.level.getEntity(packet.getId());
    if (!(entity instanceof Player player)) return;
    BlockPos pos = packet.getPos();
    if (pos == null) return;
    if (minecraft.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
        > range.getValue() * range.getValue()) {
      return;
    }
    TrackedBreak current = breaking.get(player.getUUID());
    if (packet.getProgress() < 0) {
      if (current != null && current.pos.equals(pos)) {
        breaking.remove(player.getUUID());
      }
      return;
    }
    if (current != null && current.pos.equals(pos)) {
      current.stage = Math.max(current.stage, packet.getProgress());
      return;
    }
    breaking.put(player.getUUID(), new TrackedBreak(pos.immutable(), packet.getProgress()));
    DebugLogger.get()
        .logFile(
            getLabel(),
            "tracking " + player.getName().getString() + " mining " + pos.toShortString());
  }

  /** Our own digs never come back as break-anim packets, so seed them from our packets. */
  private void onSelfDig(ServerboundPlayerActionPacket packet) {
    if (minecraft.level == null || minecraft.player == null) return;
    if (packet.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
      breaking.put(minecraft.player.getUUID(), new TrackedBreak(packet.getPos().immutable(), 0));
    } else if (packet.getAction() == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
      breaking.remove(minecraft.player.getUUID());
    }
  }

  private void onRender() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (breaking.isEmpty()) return;
    double rangeSq = range.getValue() * range.getValue();
    double playerSq = playerRange.getValue() * playerRange.getValue();

    var it = breaking.entrySet().iterator();
    while (it.hasNext()) {
      var entry = it.next();
      Player player = playerByUuid(entry.getKey());
      TrackedBreak tracked = entry.getValue();
      if (player == null
          || !player.isAlive()
          || minecraft.level.getBlockState(tracked.pos).isAir()) {
        it.remove();
        continue;
      }
      if (minecraft.player.distanceToSqr(
              tracked.pos.getX() + 0.5, tracked.pos.getY() + 0.5, tracked.pos.getZ() + 0.5)
          > rangeSq) {
        continue;
      }
      if (player.distanceToSqr(
              tracked.pos.getX() + 0.5, tracked.pos.getY() + 0.5, tracked.pos.getZ() + 0.5)
          > playerSq) {
        it.remove();
        continue;
      }

      boolean filled =
          renderMode.getValue() == RenderMode.FILLED || renderMode.getValue() == RenderMode.BOTH;
      boolean outlined =
          renderMode.getValue() == RenderMode.OUTLINED || renderMode.getValue() == RenderMode.BOTH;
      int color = 0xFF00FF00;
      int fillColor =
          ARGB.color(
              useCustomAlpha.getValue()
                  ? Math.round(fillAlpha.getValue())
                  : EspRenderManager.getGlobalFillAlpha(),
              color);
      int outlineColor =
          ARGB.color(
              useCustomAlpha.getValue()
                  ? Math.round(outlineAlpha.getValue())
                  : EspRenderManager.getGlobalOutlineAlpha(),
              color);
      GizmoStyle style;
      if (filled && outlined) {
        style = GizmoStyle.strokeAndFill(outlineColor, lineWidth.getValue(), fillColor);
      } else if (filled) {
        style = GizmoStyle.fill(fillColor);
      } else {
        style = GizmoStyle.stroke(outlineColor, lineWidth.getValue());
      }
      Gizmos.cuboid(new AABB(tracked.pos), style).setAlwaysOnTop();

      String name = player.getName().getString();
      Vec3 tagPos =
          new Vec3(tracked.pos.getX() + 0.5, tracked.pos.getY() + 0.5, tracked.pos.getZ() + 0.5);
      if (showProgress.getValue()) {
        int pct = interpolatedPercent(player, tracked);
        float hue = pct / 300.0f;
        float lineH = 0.625f * textScale.getValue();
        int progressColor = 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f) & 0xFFFFFF;
        net.minecraft.gizmos.Gizmos.billboardText(
                name,
                tagPos.add(0, lineH / 2.0f, 0),
                net.minecraft.gizmos.TextGizmo.Style.whiteAndCentered()
                    .withScale(textScale.getValue()))
            .setAlwaysOnTop();
        net.minecraft.gizmos.Gizmos.billboardText(
                pct + "%",
                tagPos.subtract(0, lineH / 2.0f, 0),
                net.minecraft.gizmos.TextGizmo.Style.forColorAndCentered(progressColor)
                    .withScale(textScale.getValue()))
            .setAlwaysOnTop();
      } else {
        net.minecraft.gizmos.Gizmos.billboardText(
                name,
                tagPos,
                net.minecraft.gizmos.TextGizmo.Style.whiteAndCentered()
                    .withScale(textScale.getValue()))
            .setAlwaysOnTop();
      }
    }
  }

  /** Chases the target percent so discrete server stages count up smoothly. */
  private int interpolatedPercent(Player player, TrackedBreak tracked) {
    int target = progressPercent(player, tracked);
    long now = System.currentTimeMillis();
    float dt = Math.max(0, now - tracked.lastUpdate) / 1000.0f;
    tracked.lastUpdate = now;
    float rate = 1.0f - (float) Math.exp(-dt * 20.0f);
    tracked.shown += (target - tracked.shown) * rate;
    if (tracked.shown > target) tracked.shown = target;
    return Math.round(tracked.shown);
  }

  /**
   * Server stages for other players; elapsed-time estimate for ourselves (our own breaks never come
   * back as packets, and remote inventories aren't readable client-side).
   */
  private int progressPercent(Player player, TrackedBreak tracked) {
    int staged = tracked.stage >= 9 ? 100 : (tracked.stage + 1) * 10;
    if (player != minecraft.player) return staged;
    long breakMs =
        me.friendly.exeter.util.BreakUtil.estimateBreakMs(minecraft.level, player, tracked.pos);
    if (breakMs <= 0) return staged;
    return (int)
        Math.max(
            staged, Math.min(100, (System.currentTimeMillis() - tracked.start) * 100L / breakMs));
  }

  private Player playerByUuid(UUID uuid) {
    for (Player player : minecraft.level.players()) {
      if (player.getUUID().equals(uuid)) return player;
    }
    return null;
  }
}
