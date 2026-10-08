package me.friendly.exeter.module.impl.active.render;

import java.awt.*;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.PopupProperty;

/**
 * This class is not present in the original Exeter 1.8 client. It was added as part of the Fabric
 * 1.21.11 port
 *
 * @author Gopro336
 */
public final class Colors extends Module {

  public enum HudColorMode {
    DEFAULT,
    CLIENT
  }

  public enum ColorMode {
    RGB,
    HSL,
    RAINBOW,
    GRADIENT
  }

  private static final EnumProperty<HudColorMode> hudColorMode =
      new EnumProperty<>(HudColorMode.DEFAULT, "HUD Color", "HUDColor", "HUDMode");
  private static final EnumProperty<ColorMode> colorMode =
      new EnumProperty<>(ColorMode.HSL, "Color Mode", "colormode", "mode");
  private static final NumberProperty<Integer> red =
      new NumberProperty<>(162, 0, 255, "Red", "red", "r");
  private static final NumberProperty<Integer> green =
      new NumberProperty<>(232, 0, 255, "Green", "green", "g");
  private static final NumberProperty<Integer> blue =
      new NumberProperty<>(190, 0, 255, "Blue", "blue", "b");
  private final PopupProperty pickColor =
      new PopupProperty(
          "Pick Color",
          () ->
              me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui.getClickGui()
                  .openPopup(
                      new me.friendly.exeter.module.impl.toggle.render.clickgui
                          .ColorPickerPopup()));
  private static final NumberProperty<Float> hue =
      new NumberProperty<>(135.4f, 0f, 360f, "Hue", "RGB", "HSL");
  private static final NumberProperty<Float> saturation =
      new NumberProperty<>(30f, 0f, 100f, "Saturation", "RainbowSaturation");
  private static final NumberProperty<Float> lightness =
      new NumberProperty<>(
          91f,
          0f,
          100f,
          "Lightness",
          "Light",
          "Luminance",
          "Luminace",
          "Brightness",
          "Bright",
          "Brigtness",
          "Brigntrnew",
          "Brighgrtnewss");
  private static final NumberProperty<Float> rainbowSpeed =
      new NumberProperty<>(
          1f, 0f, 5f, "Rainbow Speed", "RainbowSpeed", "RainbowHueSpeed", "RainbowSped");
  private static final NumberProperty<Float> rainbowSaturation =
      new NumberProperty<>(0.5f, 0f, 1f, "Rainbow Saturation", "RainbowSaturation");
  private static final NumberProperty<Float> rainbowBrightness =
      new NumberProperty<>(1.0f, 0f, 1f, "Rainbow Brightness", "RainbowBrightness");
  private static final NumberProperty<Float> rainbowFactor =
      new NumberProperty<>(1.0f, 0f, 5f, "Rainbow Factor", "RainbowFactor");
  private static final NumberProperty<Float> gradientSpeed =
      new NumberProperty<>(1.0f, 0f, 5f, "Gradient Speed", "GradientSpeed");
  private static final NumberProperty<Float> gradientBrightness =
      new NumberProperty<>(1.0f, 0f, 1f, "Gradient Brightness", "GradientBrightness");
  private static final NumberProperty<Float> gradientFactor =
      new NumberProperty<>(1.0f, 0f, 5f, "Gradient Factor", "GradientFactor");
  private static final NumberProperty<Integer> gradientCount =
      new NumberProperty<>(2, 2, 5, "Gradient Colors", "GradientColors", "GradientCount");
  private static final NumberProperty<Float> espFillAlpha =
      new NumberProperty<>(60f, 0f, 255f, "ESP Fill Alpha", "FillAlpha");
  private static final NumberProperty<Float> espOutlineAlpha =
      new NumberProperty<>(255f, 0f, 255f, "ESP Outline Alpha", "OutlineAlpha");

  // Gradient stops. Sliders triple as config storage; the pickers write the same values.
  private static final NumberProperty<Integer> grad1Red =
      new NumberProperty<>(255, 0, 255, "Color 1 Red", "grad1red", "g1r");
  private static final NumberProperty<Integer> grad1Green =
      new NumberProperty<>(0, 0, 255, "Color 1 Green", "grad1green", "g1g");
  private static final NumberProperty<Integer> grad1Blue =
      new NumberProperty<>(255, 0, 255, "Color 1 Blue", "grad1blue", "g1b");
  private static final NumberProperty<Integer> grad2Red =
      new NumberProperty<>(0, 0, 255, "Color 2 Red", "grad2red", "g2r");
  private static final NumberProperty<Integer> grad2Green =
      new NumberProperty<>(255, 0, 255, "Color 2 Green", "grad2green", "g2g");
  private static final NumberProperty<Integer> grad2Blue =
      new NumberProperty<>(0, 0, 255, "Color 2 Blue", "grad2blue", "g2b");
  private static final NumberProperty<Integer> grad3Red =
      new NumberProperty<>(0, 0, 255, "Color 3 Red", "grad3red", "g3r");
  private static final NumberProperty<Integer> grad3Green =
      new NumberProperty<>(0, 0, 255, "Color 3 Green", "grad3green", "g3g");
  private static final NumberProperty<Integer> grad3Blue =
      new NumberProperty<>(255, 0, 255, "Color 3 Blue", "grad3blue", "g3b");
  private static final NumberProperty<Integer> grad4Red =
      new NumberProperty<>(255, 0, 255, "Color 4 Red", "grad4red", "g4r");
  private static final NumberProperty<Integer> grad4Green =
      new NumberProperty<>(255, 0, 255, "Color 4 Green", "grad4green", "g4g");
  private static final NumberProperty<Integer> grad4Blue =
      new NumberProperty<>(0, 0, 255, "Color 4 Blue", "grad4blue", "g4b");
  private static final NumberProperty<Integer> grad5Red =
      new NumberProperty<>(255, 0, 255, "Color 5 Red", "grad5red", "g5r");
  private static final NumberProperty<Integer> grad5Green =
      new NumberProperty<>(128, 0, 255, "Color 5 Green", "grad5green", "g5g");
  private static final NumberProperty<Integer> grad5Blue =
      new NumberProperty<>(0, 0, 255, "Color 5 Blue", "grad5blue", "g5b");
  private final PopupProperty pickGradient1 =      new PopupProperty(
          "Pick Color 1",
          () ->
              me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui.getClickGui()
                  .openPopup(
                      new me.friendly.exeter.module.impl.toggle.render.clickgui.ColorPickerPopup(
                          () -> gradientStop(0), v -> setGradientStop(0, v), null)));
  private final PopupProperty pickGradient2 =
      new PopupProperty(
          "Pick Color 2",
          () ->
              me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui.getClickGui()
                  .openPopup(
                      new me.friendly.exeter.module.impl.toggle.render.clickgui.ColorPickerPopup(
                          () -> gradientStop(1), v -> setGradientStop(1, v), null)));
  private final PopupProperty pickGradient3 =
      new PopupProperty(
          "Pick Color 3",
          () ->
              me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui.getClickGui()
                  .openPopup(
                      new me.friendly.exeter.module.impl.toggle.render.clickgui.ColorPickerPopup(
                          () -> gradientStop(2), v -> setGradientStop(2, v), null)));
  private final PopupProperty pickGradient4 =
      new PopupProperty(
          "Pick Color 4",
          () ->
              me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui.getClickGui()
                  .openPopup(
                      new me.friendly.exeter.module.impl.toggle.render.clickgui.ColorPickerPopup(
                          () -> gradientStop(3), v -> setGradientStop(3, v), null)));
  private final PopupProperty pickGradient5 =
      new PopupProperty(
          "Pick Color 5",
          () ->
              me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui.getClickGui()
                  .openPopup(
                      new me.friendly.exeter.module.impl.toggle.render.clickgui.ColorPickerPopup(
                          () -> gradientStop(4), v -> setGradientStop(4, v), null)));

  public Colors() {
    super("Colors", new String[] {"Colors", "Color"});
    setDescription("Configures the client accent color and rainbow effects.");
    red.visibleWhen(() -> colorMode.getValue() == ColorMode.RGB);
    green.visibleWhen(() -> colorMode.getValue() == ColorMode.RGB);
    blue.visibleWhen(() -> colorMode.getValue() == ColorMode.RGB);
    pickColor.visibleWhen(() -> colorMode.getValue() == ColorMode.RGB);
    hue.visibleWhen(() -> colorMode.getValue() == ColorMode.HSL);
    saturation.visibleWhen(() -> colorMode.getValue() == ColorMode.HSL);
    lightness.visibleWhen(() -> colorMode.getValue() == ColorMode.HSL);
    rainbowSpeed.visibleWhen(() -> colorMode.getValue() == ColorMode.RAINBOW);
    rainbowSaturation.visibleWhen(() -> colorMode.getValue() == ColorMode.RAINBOW);
    rainbowBrightness.visibleWhen(() -> colorMode.getValue() == ColorMode.RAINBOW);
    rainbowFactor.visibleWhen(() -> colorMode.getValue() == ColorMode.RAINBOW);
    gradientSpeed.visibleWhen(() -> colorMode.getValue() == ColorMode.GRADIENT);
    gradientBrightness.visibleWhen(() -> colorMode.getValue() == ColorMode.GRADIENT);
    gradientFactor.visibleWhen(() -> colorMode.getValue() == ColorMode.GRADIENT);
    gradientCount.visibleWhen(() -> colorMode.getValue() == ColorMode.GRADIENT);
    grad1Red.visibleWhen(() -> colorMode.getValue() == ColorMode.GRADIENT);
    grad1Green.visibleWhen(() -> colorMode.getValue() == ColorMode.GRADIENT);
    grad1Blue.visibleWhen(() -> colorMode.getValue() == ColorMode.GRADIENT);
    pickGradient1.visibleWhen(() -> colorMode.getValue() == ColorMode.GRADIENT);
    grad2Red.visibleWhen(() -> colorMode.getValue() == ColorMode.GRADIENT);
    grad2Green.visibleWhen(() -> colorMode.getValue() == ColorMode.GRADIENT);
    grad2Blue.visibleWhen(() -> colorMode.getValue() == ColorMode.GRADIENT);
    pickGradient2.visibleWhen(() -> colorMode.getValue() == ColorMode.GRADIENT);
    grad3Red.visibleWhen(
        () -> colorMode.getValue() == ColorMode.GRADIENT && gradientCount.getValue() >= 3);
    pickGradient3.visibleWhen(
        () -> colorMode.getValue() == ColorMode.GRADIENT && gradientCount.getValue() >= 3);
    pickGradient4.visibleWhen(
        () -> colorMode.getValue() == ColorMode.GRADIENT && gradientCount.getValue() >= 4);
    pickGradient5.visibleWhen(
        () -> colorMode.getValue() == ColorMode.GRADIENT && gradientCount.getValue() >= 5);
    // Raw stop sliders stay persisted for config, but the pickers replace them in the UI.
    grad1Red.visibleWhen(() -> false);
    grad1Green.visibleWhen(() -> false);
    grad1Blue.visibleWhen(() -> false);
    grad2Red.visibleWhen(() -> false);
    grad2Green.visibleWhen(() -> false);
    grad2Blue.visibleWhen(() -> false);
    grad3Red.visibleWhen(() -> false);
    grad3Green.visibleWhen(() -> false);
    grad3Blue.visibleWhen(() -> false);
    grad4Red.visibleWhen(() -> false);
    grad4Green.visibleWhen(() -> false);
    grad4Blue.visibleWhen(() -> false);
    grad5Red.visibleWhen(() -> false);
    grad5Green.visibleWhen(() -> false);
    grad5Blue.visibleWhen(() -> false);
    offerProperties(
        colorMode,
        red,
        green,
        blue,
        pickColor,
        hue,
        saturation,
        lightness,
        rainbowSpeed,
        rainbowSaturation,
        rainbowBrightness,
        rainbowFactor,
        gradientCount,
        grad1Red,
        grad1Green,
        grad1Blue,
        pickGradient1,
        grad2Red,
        grad2Green,
        grad2Blue,
        pickGradient2,
        grad3Red,
        grad3Green,
        grad3Blue,
        pickGradient3,
        grad4Red,
        grad4Green,
        grad4Blue,
        pickGradient4,
        grad5Red,
        grad5Green,
        grad5Blue,
        pickGradient5,
        gradientSpeed,
        gradientBrightness,
        gradientFactor,
        hudColorMode,
        espFillAlpha,
        espOutlineAlpha);
  }

  private static Color baseColor() {
    if (colorMode.getValue() == ColorMode.RGB) {
      return new Color(red.getValue(), green.getValue(), blue.getValue());
    }
    if (colorMode.getValue() == ColorMode.RAINBOW) {
      return rainbowColor(rainbowScroll());
    }
    if (colorMode.getValue() == ColorMode.GRADIENT) {
      return gradientColor(gradientScroll());
    }
    return new Color(
        Color.HSBtoRGB(hue.getValue(), saturation.getValue() / 100f, lightness.getValue() / 100f));
  }

  /** Shared time scrolls, 0-1 over each mode's cycle. */
  private static float rainbowScroll() {
    long cycleMs = Math.max(500L, Math.round((6.0f - rainbowSpeed.getValue()) * 1000.0f));
    return (float) (System.currentTimeMillis() % cycleMs) / (float) cycleMs;
  }

  private static float gradientScroll() {
    long cycleMs = Math.max(500L, Math.round((6.0f - gradientSpeed.getValue()) * 1000.0f));
    return (float) (System.currentTimeMillis() % cycleMs) / (float) cycleMs;
  }

  private static Color rainbowColor(float t) {
    return Color.getHSBColor(t, rainbowSaturation.getValue(), rainbowBrightness.getValue());
  }

  /** Interpolates through the gradient stops, wrapping around. */
  private static Color gradientColor(float t) {
    int count = Math.max(2, Math.min(5, gradientCount.getValue()));
    float scaled = t * count;
    int index = (int) Math.floor(scaled) % count;
    float local = scaled - (float) Math.floor(scaled);
    int first = gradientStop(index);
    int second = gradientStop((index + 1) % count);
    int r = Math.round((((first >> 16) & 0xFF) * (1 - local)) + (((second >> 16) & 0xFF) * local));
    int g = Math.round((((first >> 8) & 0xFF) * (1 - local)) + (((second >> 8) & 0xFF) * local));
    int b = Math.round((((first) & 0xFF) * (1 - local)) + (((second) & 0xFF) * local));
    float[] hsb = Color.RGBtoHSB(r, g, b, null);
    return new Color(
        Color.HSBtoRGB(hsb[0], hsb[1], Math.min(hsb[2], gradientBrightness.getValue())));
  }

  private static int gradientStop(int index) {
    return switch (index) {
      case 0 -> pack(grad1Red.getValue(), grad1Green.getValue(), grad1Blue.getValue());
      case 1 -> pack(grad2Red.getValue(), grad2Green.getValue(), grad2Blue.getValue());
      case 2 -> pack(grad3Red.getValue(), grad3Green.getValue(), grad3Blue.getValue());
      case 3 -> pack(grad4Red.getValue(), grad4Green.getValue(), grad4Blue.getValue());
      default -> pack(grad5Red.getValue(), grad5Green.getValue(), grad5Blue.getValue());
    };
  }

  private static void setGradientStop(int index, int rgb) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    switch (index) {
      case 0 -> {
        grad1Red.setValue(r);
        grad1Green.setValue(g);
        grad1Blue.setValue(b);
      }
      case 1 -> {
        grad2Red.setValue(r);
        grad2Green.setValue(g);
        grad2Blue.setValue(b);
      }
      case 2 -> {
        grad3Red.setValue(r);
        grad3Green.setValue(g);
        grad3Blue.setValue(b);
      }
      case 3 -> {
        grad4Red.setValue(r);
        grad4Green.setValue(g);
        grad4Blue.setValue(b);
      }
      default -> {
        grad5Red.setValue(r);
        grad5Green.setValue(g);
        grad5Blue.setValue(b);
      }
    }
  }

  private static int pack(int r, int g, int b) {
    return (r << 16) | (g << 8) | b;
  }

  public static int getClientColorCustomAlpha(int alpha) {
    return setAlpha(baseColor(), alpha).getRGB();
  }

  // used for clickgui
  public static int getDarkerClientColorCustomAlpha(int alpha) {
    Color base = baseColor();
    float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
    Color darker =
        new Color(Color.HSBtoRGB(hsb[0], hsb[1], Math.max(0f, Math.min(1f, hsb[2] * 0.4f))));
    return setAlpha(darker, alpha).getRGB();
  }

  public static final Color setAlpha(Color color, int alpha) {
    alpha = Math.clamp(alpha, 0, 255);
    return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
  }

  public static Color getRainbow(int speed, int offset, float s, float brightness) {
    float hue = (System.currentTimeMillis() + offset) % speed;
    hue /= speed;
    return Color.getHSBColor(hue, s, brightness);
  }

  /**
   * Phobos-style rolling sweep: the base hue scrolls with time plus a per-row offset, so
   * vertical gradients move through the spectrum. Static modes return their base color.
   */
  public static int getRollingColor(int y, int screenHeight, int alpha) {
    Color color;
    if (colorMode.getValue() == ColorMode.GRADIENT) {
      float row = gradientScroll() + ((float) y / Math.max(1, screenHeight)) * gradientFactor.getValue();
      row -= (float) Math.floor(row);
      color = gradientColor(row);
    } else if (colorMode.getValue() == ColorMode.RAINBOW) {
      float rowHue =
          rainbowScroll() + ((float) y / Math.max(1, screenHeight)) * rainbowFactor.getValue();
      rowHue -= (float) Math.floor(rowHue);
      color = rainbowColor(rowHue);
    } else {
      color = baseColor();
    }
    return setAlpha(color, alpha).getRGB();
  }

  public static void setRgb(int r, int g, int b) {
    red.setValue(Math.max(0, Math.min(255, r)));
    green.setValue(Math.max(0, Math.min(255, g)));
    blue.setValue(Math.max(0, Math.min(255, b)));
  }

  public static void useRgbMode() {
    colorMode.setValue(ColorMode.RGB);
  }

  public static int getClientColor() {
    return baseColor().getRGB();
  }

  public static int getEspFillAlpha() {
    return Math.round(espFillAlpha.getValue());
  }

  public static int getEspOutlineAlpha() {
    return Math.round(espOutlineAlpha.getValue());
  }

  public static int getHudMain() {
    if (colorMode.getValue() == ColorMode.RAINBOW) {
      int cycleMs = Math.max(500, Math.round((6.0f - rainbowSpeed.getValue()) * 1000.0f));
      return getRainbow(cycleMs, 0, rainbowSaturation.getValue(), 1.0f).getRGB();
    }
    if (hudColorMode.getValue() == HudColorMode.CLIENT) {
      return getClientColor();
    }
    return 0xFFFFFFFF;
  }

  public static int getHudAccent() {
    if (hudColorMode.getValue() == HudColorMode.DEFAULT) {
      return 0xFFAAAAAA;
    }
    Color base = baseColor();
    float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
    return new Color(Color.HSBtoRGB(hsb[0], hsb[1], Math.max(0f, Math.min(1f, hsb[2] * 0.55f))))
        .getRGB();
  }

  public static int getClientColorEsp(int fillAlpha, int outlineAlpha) {
    Color base = baseColor();
    return setAlpha(base, fillAlpha).getRGB();
  }
}
