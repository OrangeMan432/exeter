package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.waypoint.Waypoint;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws enabled waypoints in the current dimension as full-height beams, with name, coords and live
 * distance above each one. Rendered every frame with no distance culling, so far waypoints stay
 * visible.
 */
public class Waypoints extends ToggleableModule {

  private final NumberProperty<Float> lineWidth =
      new NumberProperty<Float>(2.0f, 0.5f, 8.0f, "Line Width");
  private final NumberProperty<Float> textScale =
      new NumberProperty<Float>(0.4f, 0.25f, 4.0f, "Text Scale");
  private final Property<Boolean> showCoords = new Property<Boolean>(true, "Show Coords");
  private final Property<Boolean> showDistance = new Property<Boolean>(true, "Show Distance");

  public Waypoints() {
    super("Waypoints", new String[] {"waypoints", "wp"}, 0x55FFFF, ModuleType.RENDER);
    setDescription("Beacons for saved waypoints, with coords and distance.");
    offerProperties(lineWidth, textScale, showCoords, showDistance);
    lineWidth.setDescription("Outline thickness in pixels.");
    textScale.setDescription("Size of the waypoint label text.");
    showCoords.setDescription("Shows waypoint coordinates in the label.");
    showDistance.setDescription("Shows live distance in the waypoint label.");
    this.listeners.add(
        new Listener<WorldRenderEvent>("waypoints_render") {
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
            "enabled: "
                + Exeter.getInstance().getWaypointManager().getRegistry().size()
                + " waypoints");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  private void onRender() {
    if (minecraft.level == null || minecraft.player == null) return;
    String dimension = minecraft.level.dimension().identifier().toString();
    int minY = minecraft.level.getMinY();
    int maxY = minecraft.level.getMaxY();
    float textScaleValue = textScale.getValue();

    for (Waypoint waypoint : Exeter.getInstance().getWaypointManager().getRegistry()) {
      if (!waypoint.isEnabled()) continue;
      if (!waypoint.getDimension().equals(dimension)) continue;
      // Block core, not pos + 0.5: pinned waypoints store exact player doubles.
      double x = Math.floor(waypoint.getX()) + 0.5;
      double z = Math.floor(waypoint.getZ()) + 0.5;
      int beamColor = ARGB.color(160, 0xFF000000 | waypoint.getColor());
      Gizmos.line(new Vec3(x, minY, z), new Vec3(x, maxY, z), beamColor, lineWidth.getValue())
          .setAlwaysOnTop();

      long dist =
          Math.round(
              Math.sqrt(
                  minecraft.player.distanceToSqr(
                      waypoint.getX(), waypoint.getY(), waypoint.getZ())));
      java.util.List<String> lines = new java.util.ArrayList<>(3);
      java.util.List<Integer> colors = new java.util.ArrayList<>(3);
      lines.add(waypoint.getName());
      colors.add(0xFF000000 | waypoint.getColor());
      if (showCoords.getValue()) {
        lines.add("[" + waypoint.coordsShort() + "]");
        colors.add(0xFFFFFFFF);
      }
      if (showDistance.getValue()) {
        lines.add("(" + dist + "m)");
        colors.add(0xFFFFFFFF);
      }
      me.friendly.exeter.render.TagRenderer.drawTag(
          new Vec3(x, waypoint.getY() + 3.0, z), lines, colors, textScaleValue);
    }
  }
}
