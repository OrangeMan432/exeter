package me.friendly.exeter.module.impl.active.render;

import java.awt.*;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;

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
    HSL
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
  private static final Property<Boolean> hudRainbow =
      new Property<>(false, "HUD Rainbow", "HUDRainbow", "Rainbow", "Cycle");
  private static final NumberProperty<Float> rainbowSpeed =
      new NumberProperty<>(
          1f, 0f, 5f, "RainbowSpeed", "RainbowHueSpeed", "RainbowSped", "RrainbowSpeed");
  private static final NumberProperty<Float> rainbowSaturation =
      new NumberProperty<>(0.5f, 0f, 1f, "Rainbow Saturation", "RainbowSaturation");
  private static final NumberProperty<Float> espFillAlpha =
      new NumberProperty<>(60f, 0f, 255f, "ESP Fill Alpha", "FillAlpha");
  private static final NumberProperty<Float> espOutlineAlpha =
      new NumberProperty<>(255f, 0f, 255f, "ESP Outline Alpha", "OutlineAlpha");

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
    offerProperties(
        colorMode,
        red,
        green,
        blue,
        pickColor,
        hue,
        saturation,
        lightness,
        hudColorMode,
        hudRainbow,
        rainbowSpeed,
        rainbowSaturation,
        espFillAlpha,
        espOutlineAlpha);
  }

  private static Color baseColor() {
    if (colorMode.getValue() == ColorMode.RGB) {
      return new Color(red.getValue(), green.getValue(), blue.getValue());
    }
    return new Color(
        Color.HSBtoRGB(hue.getValue(), saturation.getValue() / 100f, lightness.getValue() / 100f));
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
    if (hudRainbow.getValue()) {
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
