package me.friendly.exeter.util;

/** Float/double clamps; Mth.clamp was removed in 26.4-snapshot-3. */
public final class MathUtil {
  private MathUtil() {}

  public static float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }

  public static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  public static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  public static long clamp(long value, long min, long max) {
    return Math.max(min, Math.min(max, value));
  }
}
