package me.friendly.exeter.module.impl.active.render;

import java.awt.Color;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;

/** Client accent color and rainbow effects. */
public final class Colors extends Module {

  private static final NumberProperty<Float> hue =
      new NumberProperty<Float>(135.4f, 0f, 360f, "Hue", "RGB", "HSL");
  private static final NumberProperty<Float> saturation =
      new NumberProperty<Float>(30f, 0f, 100f, "Saturation", "RainbowSaturation");
  private static final NumberProperty<Float> lightness =
      new NumberProperty<Float>(
          91f, 0f, 100f, "Lightness", "Light", "Luminance", "Brightness", "Bright");
  private final Property<Boolean> hudRainbow =
      new Property<Boolean>(false, "HUD Rainbow", "HUDRainbow", "Rainbow", "Cycle");
  private final NumberProperty<Float> rainbowSpeed =
      new NumberProperty<Float>(1f, 0f, 5f, "RainbowSpeed", "RainbowHueSpeed");
  private final NumberProperty<Float> rainbowHue =
      new NumberProperty<Float>(4f, 0f, 10f, "RainbowHue", "RainbowHueSpeed2");
  private static final NumberProperty<Float> espFillAlpha =
      new NumberProperty<Float>(60f, 0f, 255f, "ESP Fill Alpha", "FillAlpha");
  private static final NumberProperty<Float> espOutlineAlpha =
      new NumberProperty<Float>(255f, 0f, 255f, "ESP Outline Alpha", "OutlineAlpha");

  public Colors() {
    super("Colors", new String[] {"Colors", "Color"});
    setDescription("Configures the client accent color and rainbow effects.");
    offerProperties(
        hue,
        saturation,
        lightness,
        hudRainbow,
        rainbowSpeed,
        rainbowHue,
        espFillAlpha,
        espOutlineAlpha);
  }

  public static int getClientColorCustomAlpha(int alpha) {
    Color color =
        setAlpha(
            new Color(
                Color.HSBtoRGB(
                    hue.getValue().floatValue(),
                    saturation.getValue().floatValue() / 100f,
                    lightness.getValue().floatValue() / 100f)),
            alpha);
    return color.getRGB();
  }

  // used for clickgui
  public static int getDarkerClientColorCustomAlpha(int alpha) {
    Color color =
        setAlpha(
            new Color(
                Color.HSBtoRGB(
                    hue.getValue().floatValue(),
                    saturation.getValue().floatValue() / 100f,
                    lightness.getValue().floatValue() / 250f)),
            alpha);
    return color.getRGB();
  }

  public static Color setAlpha(Color color, int alpha) {
    if (alpha < 0) alpha = 0;
    if (alpha > 255) alpha = 255;
    return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
  }

  public static Color getRainbow(int speed, int offset, float s, float brightness) {
    float hueValue = (System.currentTimeMillis() + offset) % speed;
    hueValue /= speed;
    return Color.getHSBColor(hueValue, s, brightness);
  }

  public static int getClientColor() {
    return Color.getHSBColor(
            hue.getValue().floatValue(),
            saturation.getValue().floatValue() / 100f,
            lightness.getValue().floatValue() / 100f)
        .getRGB();
  }

  public static int getEspFillAlpha() {
    return Math.round(espFillAlpha.getValue().floatValue());
  }

  public static int getEspOutlineAlpha() {
    return Math.round(espOutlineAlpha.getValue().floatValue());
  }
}
