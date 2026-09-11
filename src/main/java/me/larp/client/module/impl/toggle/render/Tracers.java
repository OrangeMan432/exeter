package me.larp.client.module.impl.toggle.render;

import me.larp.api.event.Listener;
import me.larp.client.events.RenderWorldEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.Render3D;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Draws lines from your crosshair to nearby players. */
public class Tracers extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(64.0, 8.0, 256.0, "Range");
  private final NumberProperty<Double> lineWidth =
      new NumberProperty<Double>(2.0, 1.0, 5.0, "Line Width");
  private final Property<Boolean> players =
      new Property<Boolean>(true, "Players");
  private final Property<Boolean> hostiles =
      new Property<Boolean>(false, "Hostiles");

  public Tracers() {
    super("Tracers", new String[] {"tracers", "lines"}, 0xFF00FF, ModuleType.RENDER);
    setDescription("Lines pointing at entities.");
    offerProperties(range, lineWidth, players, hostiles);
    this.listeners.add(
        new Listener<RenderWorldEvent>("tracers_render") {
          @Override
          public void call(RenderWorldEvent event) {
            Tracers.this.onRender(event);
          }
        });
  }

  private void onRender(RenderWorldEvent event) {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.hitResult == null) return;
    Vec3 from = minecraft.hitResult.getLocation();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player || !entity.isAlive()) continue;
      boolean isPlayer = entity instanceof Player;
      if (isPlayer && !players.getValue()) continue;
      if (!isPlayer && !hostiles.getValue()) continue;
      if (minecraft.player.distanceTo(entity) > range.getValue()) continue;
      Vec3 target = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
      int color = isPlayer ? 0xFFFF5555 : 0xFFFFAA00;
      Render3D.drawTracer(
          event.getSubmitNodeStorage(),
          event.getCamera(),
          event.getMatrixStack(),
          from,
          target,
          color,
          lineWidth.getValue().floatValue());
    }
  }
}
