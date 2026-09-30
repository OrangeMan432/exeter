package me.friendly.exeter.util;

/** Client accent colors until the Colors module is ported. */
public final class ClientColors {
  private ClientColors() {}

  public static int getClientColor() {
    return 0xFFAA2222;
  }

  public static int getDarkerClientColor() {
    return 0xFF661111;
  }

  public static int getClientColorCustomAlpha(int alpha) {
    return (alpha << 24) | (getClientColor() & 0xFFFFFF);
  }

  public static int getDarkerClientColorCustomAlpha(int alpha) {
    return (alpha << 24) | (getDarkerClientColor() & 0xFFFFFF);
  }
}
