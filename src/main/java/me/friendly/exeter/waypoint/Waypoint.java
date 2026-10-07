package me.friendly.exeter.waypoint;

/** A named world position. Rendering is toggled per waypoint, not by a module switch. */
public class Waypoint {
  private final String name;
  private final double x;
  private final double y;
  private final double z;
  private final String dimension;
  private boolean enabled;
  private int color;

  public Waypoint(String name, double x, double y, double z, String dimension, boolean enabled) {
    this(name, x, y, z, dimension, enabled, randomColor());
  }

  public Waypoint(
      String name, double x, double y, double z, String dimension, boolean enabled, int color) {
    this.name = name;
    this.x = x;
    this.y = y;
    this.z = z;
    this.dimension = dimension;
    this.enabled = enabled;
    this.color = color;
  }

  public String getName() {
    return name;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getZ() {
    return z;
  }

  public String getDimension() {
    return dimension;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /** Packed 0xRRGGBB waypoint color. */
  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color & 0xFFFFFF;
  }

  public String coordsShort() {
    return (int) Math.floor(x) + ", " + (int) Math.floor(y) + ", " + (int) Math.floor(z);
  }

  /** Vivid random color for new waypoints. */
  public static int randomColor() {
    return java.awt.Color.HSBtoRGB(
            java.util.concurrent.ThreadLocalRandom.current().nextFloat(), 1.0f, 1.0f)
        & 0xFFFFFF;
  }
}
