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
import net.minecraft.gizmos.TextGizmo;
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
      new NumberProperty<Float>(1.0f, 0.25f, 4.0f, "Text Scale");
  private final Property<Boolean> showCoords = new Property<Boolean>(true, "Show Coords");
  private final Property<Boolean> showDistance = new Property<Boolean>(true, "Show Distance");

  public Waypoints() {
    super("Waypoints", new String[] {"waypoints", "wp"}, 0x55FFFF, ModuleType.RENDER);
    setDescription("Beacons for saved waypoints, with coords and distance.");
    offerProperties(lineWidth, textScale, showCoords, showDistance);
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

    // Camera basis for the nametag backdrop quad. Right-handed: facing yaw, right is
    // 90 degrees clockwise from forward (west when facing south), up is right x forward.
    float yaw = (float) Math.toRadians(minecraft.player.getYRot());
    float pitch = (float) Math.toRadians(minecraft.player.getXRot());
    float sinY = (float) Math.sin(yaw);
    float cosY = (float) Math.cos(yaw);
    float cosP = (float) Math.cos(pitch);
    Vec3 forward = new Vec3(-sinY * cosP, -(float) Math.sin(pitch), cosY * cosP);
    Vec3 right = new Vec3(-cosY, 0f, -sinY);
    Vec3 up = right.cross(forward).normalize();

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

      // Plate metrics in world units: the gizmo text pipeline draws font pixels at
      // scale/16, so a 9px line is 9/16 tall. Generous char width keeps the plate wide.
      float lineH = 0.625f * textScaleValue;
      float charW = 0.375f * textScaleValue;
      int maxLen = 1;
      for (String line : lines) {
        maxLen = Math.max(maxLen, line.length());
      }
      float halfW = maxLen * charW / 2.0f + 0.15f * textScaleValue;
      float halfH = (lines.size() * lineH) / 2.0f + 0.1f * textScaleValue;
      Vec3 center = new Vec3(x, waypoint.getY() + 3.0, z);
      // Glyphs extend up from their baseline, so the plate sits a quarter line lower
      // than the line centers.
      Vec3 plateCenter = center.subtract(up.scale(lineH * 0.25f));
      // Text rides slightly toward the viewer so translucent sorting against the plate
      // stays stable instead of swapping frame to frame.
      Vec3 textPush = forward.scale(-0.05);
      Vec3 rightScaled = right.scale(halfW);
      Vec3 upScaled = up.scale(halfH);
      Vec3 topLeft = plateCenter.add(upScaled).subtract(rightScaled);
      Vec3 topRight = plateCenter.add(upScaled).add(rightScaled);
      Vec3 bottomRight = plateCenter.subtract(upScaled).add(rightScaled);
      Vec3 bottomLeft = plateCenter.subtract(upScaled).subtract(rightScaled);
      var plate = net.minecraft.gizmos.GizmoStyle.fill(0x40000000);
      Gizmos.rect(topLeft, topRight, bottomRight, bottomLeft, plate).setAlwaysOnTop();
      Gizmos.rect(topLeft, bottomLeft, bottomRight, topRight, plate).setAlwaysOnTop();

      for (int i = 0; i < lines.size(); i++) {
        float dy = ((lines.size() - 1) / 2.0f - i) * lineH;
        var lineStyle =
            TextGizmo.Style.forColorAndCentered(colors.get(i)).withScale(textScaleValue);
        Vec3 anchor = new Vec3(center.x, center.y + dy, center.z).add(textPush);
        Gizmos.billboardText(lines.get(i), anchor, lineStyle).setAlwaysOnTop();
      }
    }
  }
}
