package me.larp.client.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.List;
import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.RenderWorldEvent;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.Render3D;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Saved-location markers with distance tags. Add = your feet, Clear wipes. */
public class Waypoints extends ToggleableModule {

  private final NumberProperty<Double> lineWidth =
      new NumberProperty<Double>(2.0, 1.0, 5.0, "Line Width");
  private final NumberProperty<Double> scale =
      new NumberProperty<Double>(1.0, 0.5, 3.0, "Scale");
  private final Property<Boolean> tracers =
      new Property<Boolean>(true, "Tracers");

  private final List<BlockPos> points = new ArrayList<>();

  public Waypoints() {
    super("Waypoints", new String[] {"waypoints", "wp"}, 0xFF00FF, ModuleType.RENDER);
    setDescription("Markers you drop with .wp add in chat.");
    offerProperties(lineWidth, scale, tracers);
    this.listeners.add(
        new Listener<RenderWorldEvent>("waypoints_render") {
          @Override
          public void call(RenderWorldEvent event) {
            Waypoints.this.onRender(event);
          }
        });
    // Chat-driven add/clear handled by the .wp command class.
    this.listeners.add(
        new Listener<TickEvent>("waypoints_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
          }
        });
  }

  public void addPoint(BlockPos pos) {
    points.add(pos.immutable());
  }

  public void clearPoints() {
    points.clear();
  }

  public List<BlockPos> getPoints() {
    return points;
  }

  private void onRender(RenderWorldEvent event) {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.hitResult == null) return;
    Vec3 from = minecraft.hitResult.getLocation();
    for (BlockPos pos : points) {
      Render3D.drawBoxOutline(
          event.getSubmitNodeStorage(),
          event.getCamera(),
          event.getMatrixStack(),
          new AABB(pos),
          0xFF00FF00,
          lineWidth.getValue().floatValue());
      Vec3 at = new Vec3(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5);
      double dist = minecraft.player.position().distanceTo(at);
      Render3D.drawText(
          event.getSubmitNodeStorage(),
          event.getMatrixStack(),
          event.getCamera(),
          minecraft,
          at,
          String.format("%.0fm", dist),
          0xFF00FF00,
          scale.getValue().floatValue());
      if (tracers.getValue()) {
        Render3D.drawTracer(
            event.getSubmitNodeStorage(),
            event.getCamera(),
            event.getMatrixStack(),
            from,
            at,
            0x8800FF00,
            lineWidth.getValue().floatValue());
      }
    }
  }
}
