package me.friendly.exeter.module.impl.toggle.render.clickgui;

import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.exeter.module.impl.active.render.Colors;

/**
 * Border painter shared by the ClickGUI panels and the description box.
 *
 * <p>A border sits outside its box: {@code headerTop - 1} through {@code bottom + 1}. In flat mode
 * it is drawn in the accent (or rolling hue). In gradient mode it holds the header color down the
 * header band and then fades into {@link #BACKGROUND}, so the edge reads as part of the header.
 */
public final class PanelBorder {
  public static final int BACKGROUND = 0x77000000;
  private static final int ACCENT_ALPHA = 77;
  private static final float STRIP_HEIGHT = 4.0f;

  private PanelBorder() {}

  public static void draw(
      float x,
      float width,
      float headerTop,
      float headerBottom,
      float bottom,
      int screenW,
      int screenH,
      boolean rolling,
      boolean horizontal,
      boolean inverse,
      boolean faded) {
    if (faded) {
      drawFaded(
          x,
          width,
          headerTop,
          headerBottom,
          bottom,
          screenW,
          screenH,
          rolling,
          horizontal,
          inverse);
      return;
    }
    if (horizontal) {
      RenderMethods.drawHorizontalSpectrumRect(
          x - 1,
          headerTop - 1,
          x + width + 1,
          headerTop,
          sx ->
              Colors.rollingSample(
                  true, inverse, sx, (int) headerTop, screenW, screenH, ACCENT_ALPHA));
      RenderMethods.drawHorizontalSpectrumRect(
          x - 1,
          bottom,
          x + width + 1,
          bottom + 1,
          sx ->
              Colors.rollingSample(
                  true, inverse, sx, (int) bottom, screenW, screenH, ACCENT_ALPHA));
      RenderMethods.drawRect(
          x - 1,
          headerTop,
          x,
          bottom,
          Colors.rollingSample(
              true, inverse, (int) (x - 1), (int) headerTop, screenW, screenH, ACCENT_ALPHA));
      RenderMethods.drawRect(
          x + width,
          headerTop,
          x + width + 1,
          bottom,
          Colors.rollingSample(
              true, inverse, (int) (x + width), (int) headerTop, screenW, screenH, ACCENT_ALPHA));
      return;
    }
    int accentTop =
        rolling
            ? Colors.rollingSample(
                false, inverse, (int) x, (int) headerTop, screenW, screenH, ACCENT_ALPHA)
            : Colors.getClientColorCustomAlpha(ACCENT_ALPHA);
    int accentBottom =
        rolling
            ? Colors.rollingSample(
                false, inverse, (int) x, (int) bottom, screenW, screenH, ACCENT_ALPHA)
            : Colors.getClientColorCustomAlpha(ACCENT_ALPHA);
    RenderMethods.drawRect(x - 1, headerTop - 1, x + width + 1, headerTop, accentTop);
    RenderMethods.drawRect(x - 1, bottom, x + width + 1, bottom + 1, accentBottom);
    // Sampled per strip: a single RGB-lerped gradient drifts off-hue mid-panel,
    // growing with panel length, while buttons sample exact hues.
    for (float sy = headerTop; sy < bottom; sy += STRIP_HEIGHT) {
      float ey = Math.min(sy + STRIP_HEIGHT, bottom);
      int stripTop =
          rolling
              ? Colors.rollingSample(
                  false, inverse, (int) x, (int) sy, screenW, screenH, ACCENT_ALPHA)
              : accentTop;
      int stripBottom =
          rolling
              ? Colors.rollingSample(
                  false, inverse, (int) x, (int) ey, screenW, screenH, ACCENT_ALPHA)
              : accentBottom;
      RenderMethods.drawGradientRect(x - 1, sy, x, ey, stripTop, stripBottom);
      RenderMethods.drawGradientRect(x + width, sy, x + width + 1, ey, stripTop, stripBottom);
    }
  }

  private static void drawFaded(
      float x,
      float width,
      float headerTop,
      float headerBottom,
      float bottom,
      int screenW,
      int screenH,
      boolean rolling,
      boolean horizontal,
      boolean inverse) {
    // Top edge: a hue sweep matches a horizontal-rolling header, otherwise the accent.
    if (rolling && horizontal) {
      RenderMethods.drawHorizontalSpectrumRect(
          x - 1,
          headerTop - 1,
          x + width + 1,
          headerTop,
          sx ->
              Colors.rollingSample(
                  true, inverse, sx, (int) headerTop, screenW, screenH, ACCENT_ALPHA));
    } else {
      int topAccent =
          rolling
              ? Colors.rollingSample(
                  false, inverse, (int) x, (int) headerTop, screenW, screenH, ACCENT_ALPHA)
              : Colors.getClientColorCustomAlpha(ACCENT_ALPHA);
      RenderMethods.drawRect(x - 1, headerTop - 1, x + width + 1, headerTop, topAccent);
    }
    // Bottom edge settles on the body color, so the border merges into the box.
    RenderMethods.drawRect(x - 1, bottom, x + width + 1, bottom + 1, BACKGROUND);

    float fadeStart = Math.min(bottom, Math.max(headerTop, headerBottom));
    float fadeSpan = Math.max(1.0f, bottom - fadeStart);
    if (!rolling) {
      int accent = Colors.getClientColorCustomAlpha(ACCENT_ALPHA);
      drawSideFade(x, width, headerTop, fadeStart, bottom, accent, accent);
      return;
    }
    if (horizontal) {
      // The hue only depends on x, so each side holds one color down the header band.
      int leftAccent =
          Colors.rollingSample(
              true, inverse, (int) (x - 1), (int) headerTop, screenW, screenH, ACCENT_ALPHA);
      int rightAccent =
          Colors.rollingSample(
              true, inverse, (int) (x + width), (int) headerTop, screenW, screenH, ACCENT_ALPHA);
      drawSideFade(x, width, headerTop, fadeStart, bottom, leftAccent, rightAccent);
      return;
    }
    // Vertical rolling: sample per strip so the hue stays exact through the whole border.
    for (float sy = headerTop; sy < bottom; sy += STRIP_HEIGHT) {
      float ey = Math.min(sy + STRIP_HEIGHT, bottom);
      float tTop = Math.max(0.0f, (sy - fadeStart) / fadeSpan);
      float tBottom = Math.max(0.0f, (ey - fadeStart) / fadeSpan);
      int stripTop =
          fadeToBackground(
              Colors.rollingSample(
                  false, inverse, (int) x, (int) sy, screenW, screenH, ACCENT_ALPHA),
              tTop);
      int stripBottom =
          fadeToBackground(
              Colors.rollingSample(
                  false, inverse, (int) x, (int) ey, screenW, screenH, ACCENT_ALPHA),
              tBottom);
      RenderMethods.drawGradientRect(x - 1, sy, x, ey, stripTop, stripBottom);
      RenderMethods.drawGradientRect(x + width, sy, x + width + 1, ey, stripTop, stripBottom);
    }
  }

  /** Solid side band alongside the header, then a single gradient down to the body color. */
  private static void drawSideFade(
      float x,
      float width,
      float headerTop,
      float fadeStart,
      float bottom,
      int leftAccent,
      int rightAccent) {
    RenderMethods.drawRect(x - 1, headerTop, x, fadeStart, leftAccent);
    RenderMethods.drawRect(x + width, headerTop, x + width + 1, fadeStart, rightAccent);
    RenderMethods.drawGradientRect(x - 1, fadeStart, x, bottom, leftAccent, BACKGROUND);
    RenderMethods.drawGradientRect(
        x + width, fadeStart, x + width + 1, bottom, rightAccent, BACKGROUND);
  }

  /** Fade from an accent color to {@link #BACKGROUND}: {@code t = 0} keeps the accent. */
  private static int fadeToBackground(int color, float t) {
    float clamped = Math.max(0.0f, Math.min(1.0f, t));
    int alpha = lerpChannel(color >>> 24, BACKGROUND >>> 24, clamped);
    int red = lerpChannel(color >> 16, BACKGROUND >> 16, clamped);
    int green = lerpChannel(color >> 8, BACKGROUND >> 8, clamped);
    int blue = lerpChannel(color, BACKGROUND, clamped);
    return (alpha << 24) | (red << 16) | (green << 8) | blue;
  }

  private static int lerpChannel(int from, int to, float t) {
    int start = from & 0xFF;
    int end = to & 0xFF;
    return Math.round(start + (end - start) * t);
  }
}
