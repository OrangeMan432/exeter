package me.friendly.exeter.util;

import java.awt.Color;

/** Client accent colors until the Colors module is ported. Matches modern defaults. */
public final class ClientColors {
  private ClientColors() {}

  private static final float HUE = 135.4f / 360f;
  private static final float SATURATION = 30f / 100f;
  private static final float LIGHTNESS = 91f / 100f;

  public static int getClientColor() {
    return getClientColorCustomAlpha(255);
  }

  public static int getDarkerClientColor() {
    return getDarkerClientColorCustomAlpha(255);
  }

  public static int getClientColorCustomAlpha(int alpha) {
    return setAlpha(new Color(Color.HSBtoRGB(HUE, SATURATION, LIGHTNESS)), alpha).getRGB();
  }

  public static int getDarkerClientColorCustomAlpha(int alpha) {
    return setAlpha(new Color(Color.HSBtoRGB(HUE, SATURATION, LIGHTNESS / 2.5f)), alpha).getRGB();
  }

  private static Color setAlpha(Color color, int alpha) {
    return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
  }
}
