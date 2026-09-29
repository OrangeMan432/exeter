package me.friendly.exeter.module.impl.active.render;

import me.friendly.exeter.module.Module;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * This class is not present in the original Exeter 1.8 client. It was added as part of the Fabric
 * 1.21.11 port
 *
 * @author Gopro336
 */
public final class Colors extends Module {

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
  private final Property<Boolean> hudRainbow =
      new Property<>(false, "HUD Rainbow", "HUDRainbow", "Rainbow", "Cycle");
  private final NumberProperty<Float> rainbowSpeed =
      new NumberProperty<>(
          1f, 0f, 5f, "RainbowSpeed", "RainbowHueSpeed", "RainbowSped", "RrainbowSpeed");
  private final NumberProperty<Float> rainbowHue =
      new NumberProperty<>(
          4f, 0f, 10f, "RainbowHue", "RainbowHueSpeed2", "RainbowSped2", "RrainbowSpeed2");
  private static final NumberProperty<Float> espFillAlpha =
      new NumberProperty<>(60f, 0f, 255f, "ESP Fill Alpha", "FillAlpha");
  private static final NumberProperty<Float> espOutlineAlpha =
      new NumberProperty<>(255f, 0f, 255f, "ESP Outline Alpha", "OutlineAlpha");

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
    return setAlpha(
        Mth.hsvToRgb(hue.getValue(), saturation.getValue() / 100f, lightness.getValue() / 100f),
        alpha);
  }

  // used for clickgui
  public static int getDarkerClientColorCustomAlpha(int alpha) {
    return setAlpha(
        Mth.hsvToRgb(hue.getValue(), saturation.getValue() / 100f, lightness.getValue() / 250f),
        alpha);
  }

  /** Replaces the alpha channel of a packed ARGB color, clamping {@code alpha} to 0-255. */
  public static int setAlpha(int color, int alpha) {
    return ARGB.color(
        Mth.clamp(alpha, 0, 255), ARGB.red(color), ARGB.green(color), ARGB.blue(color));
  }

  public static int getRainbow(int speed, int offset, float s, float brightness) {
    float hue = (System.currentTimeMillis() + offset) % speed;
    hue /= speed;
    return Mth.hsvToRgb(hue, s, brightness);
  }

  public static int getClientColor() {
    return Mth.hsvToRgb(hue.getValue(), saturation.getValue() / 100f, lightness.getValue() / 100f);
  }

  public static int getEspFillAlpha() {
    return Math.round(espFillAlpha.getValue());
  }

  public static int getEspOutlineAlpha() {
    return Math.round(espOutlineAlpha.getValue());
  }

  public static int getClientColorEsp(int fillAlpha, int outlineAlpha) {
    return setAlpha(
        Mth.hsvToRgb(hue.getValue(), saturation.getValue() / 100f, lightness.getValue() / 100f),
        fillAlpha);
  }
}
