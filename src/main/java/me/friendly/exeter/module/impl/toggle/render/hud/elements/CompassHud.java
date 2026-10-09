package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.waypoint.Waypoint;
import net.minecraft.client.Minecraft;

/**
 * FPS-style compass strip in the Phobos LINE tradition: 1px per degree, cardinal letters, center
 * caret, and a colored tick plus name for every enabled waypoint in this dimension.
 */
public final class CompassHud extends HudModule {

  public enum LabelMode {
    TOP,
    BOTTOM
  }

  private final NumberProperty<Integer> width = new NumberProperty<Integer>(120, 60, 240, "Width");
  private final EnumProperty<LabelMode> labelMode =
      new EnumProperty<LabelMode>(LabelMode.TOP, "Labels", "labelmode");

  public CompassHud() {
    super("CompassHud", new String[] {"compasshud", "compass"}, Corner.TOP_LEFT);
    setDescription("Compass strip with waypoint markers.");
    offerProperties(width, labelMode);
    width.setDescription("Sets how wide the compass strip is.");
    labelMode.setDescription("Places waypoint names above or below the strip.");
  }

  @Override
  public int getWidth() {
    return width.getValue();
  }

  @Override
  public int getHeight() {
    return 20;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null) return;
    if (!waypointsModuleOn()) return;

    int x = getX();
    int y = getY();
    int w = width.getValue();
    float yaw = mc.player.getYRot();

    // Name row above the strip, or below it with no gap on top.
    boolean below = labelMode.getValue() == LabelMode.BOTTOM;
    int stripY = below ? y : y + 9;
    float nameY = below ? stripY + 12 : y;

    // Minor ticks every 15 degrees, major at the diagonals. Cardinals get letters
    // instead of ticks.
    for (int deg = -180; deg <= 180; deg += 15) {
      if (deg % 90 == 0) continue;
      float tx = x + w / 2.0f + (float) wrapDegrees(deg - yaw);
      if (tx < x + 1 || tx > x + w - 1) continue;
      boolean major = deg % 45 == 0;
      int top = major ? stripY + 2 : stripY + 5;
      RenderMethods.drawRect(tx, top, tx + 1, stripY + 10, major ? 0xFFFFFFFF : 0xFF888888);
    }

    drawCardinal(x, w, stripY, yaw, 0.0, "S", 0xFFFFFFFF);
    drawCardinal(x, w, stripY, yaw, 90.0, "W", 0xFFFFFFFF);
    drawCardinal(x, w, stripY, yaw, -90.0, "E", 0xFFFFFFFF);
    drawCardinal(x, w, stripY, yaw, 180.0, "N", 0xFFFF5555);

    // Center caret.
    float caret = x + w / 2.0f;
    RenderMethods.drawRect(caret, stripY + 1, caret + 1, stripY + 10, 0xFFFF5555);

    String dimension = mc.level.dimension().identifier().toString();
    double px = mc.player.getX();
    double pz = mc.player.getZ();
    for (Waypoint waypoint : Exeter.getInstance().getWaypointManager().getRegistry()) {
      if (!waypoint.isEnabled()) continue;
      if (!waypoint.getDimension().equals(dimension)) continue;
      double dx = waypoint.getX() - px;
      double dz = waypoint.getZ() - pz;
      double bearing = Math.toDegrees(Math.atan2(-dx, dz));
      float mx = x + w / 2.0f + (float) wrapDegrees(bearing - yaw);
      mx = Math.max(x + 1, Math.min(x + w - 1, mx));
      int color = 0xFF000000 | waypoint.getColor();
      RenderMethods.drawRect(mx - 1, stripY + 1, mx + 2, stripY + 10, color);
      FontUtil.drawString(
          waypoint.getName(),
          mx - FontUtil.getStringWidth(waypoint.getName()) / 2.0f,
          nameY,
          color);
    }
  }

  private void drawCardinal(
      int x, int w, int stripY, float yaw, double bearing, String letter, int color) {
    float lx = x + w / 2.0f + (float) wrapDegrees(bearing - yaw);
    if (lx < x + 1 || lx > x + w - 1) return;
    FontUtil.drawString(letter, lx - FontUtil.getStringWidth(letter) / 2.0f, stripY + 2, color);
  }

  private static double wrapDegrees(double angle) {
    double wrapped = (angle + 180.0) % 360.0;
    if (wrapped < 0.0) wrapped += 360.0;
    return wrapped - 180.0;
  }

  private boolean waypointsModuleOn() {
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("waypoints");
    return module instanceof ToggleableModule toggleable && toggleable.isRunning();
  }
}
