package me.friendly.exeter.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;

/**
 * Shared fade/slide placement render, ported from 3arthh4ck's AutoCrystal ListenerRender. Modules
 * own their settings (config-safe) and pass them in as {@link Params}; this class only holds
 * per-frame state: the live cell, fade timestamps and the slide origin.
 */
public class PlacementRender {

  public enum FadeMode {
    ALPHA,
    SHRINK
  }

  /** Resolved render values; alphas are 0-255 ints (module applies its Custom Alpha gating). */
  public record Params(
      boolean fade,
      int fadeTime,
      FadeMode fadeMode,
      boolean slide,
      int slideTime,
      boolean smoothSlide,
      boolean damageText,
      int renderTime,
      float lineWidth,
      int fillAlpha,
      int outlineAlpha,
      double boxHeight) {}

  private BlockPos renderPos;
  private String renderText;
  private final Map<BlockPos, Long> fadePositions = new HashMap<>();
  private BlockPos slidePos;
  private long slideStartMs;
  private long lastUpdateMs;

  /**
   * Tracks the displayed cell, 3arthh4ck-style: the previous cell becomes the slide origin,
   * throttled by Smooth Slide.
   */
  public void setRenderPos(BlockPos pos, String text, boolean smoothSlide, int slideTime) {
    lastUpdateMs = System.currentTimeMillis();
    if (pos != null
        && !pos.equals(slidePos)
        && (!smoothSlide || lastUpdateMs - slideStartMs >= slideTime)) {
      slidePos = renderPos;
      slideStartMs = lastUpdateMs;
    }
    renderPos = pos;
    renderText = text;
  }

  public void clear() {
    renderPos = null;
    slidePos = null;
    fadePositions.clear();
  }

  public void render(Params params) {
    long now = System.currentTimeMillis();
    BlockPos pos = now - lastUpdateMs > params.renderTime() ? null : renderPos;
    int color = EspRenderManager.getClientColor();
    if (params.fade()) {
      for (Map.Entry<BlockPos, Long> stale : fadePositions.entrySet()) {
        if (pos != null && stale.getKey().equals(pos)) continue;
        float factor =
            Math.min(
                1.0f,
                Math.max(
                    0.0f,
                    (float) (stale.getValue() + params.fadeTime() - now) / params.fadeTime()));
        if (factor <= 0.0f) continue;
        if (params.fadeMode() == FadeMode.SHRINK) {
          AABB full = boxAt(stale.getKey(), params.boxHeight());
          double height = (full.maxY - full.minY) * factor;
          if (height <= 0.001) continue;
          renderBox(
              new AABB(full.minX, full.minY, full.minZ, full.maxX, full.minY + height, full.maxZ),
              params.fillAlpha(),
              params.outlineAlpha(),
              color,
              params.lineWidth());
        } else {
          renderBox(
              boxAt(stale.getKey(), params.boxHeight()),
              Math.round(params.fillAlpha() * factor),
              Math.round(params.outlineAlpha() * factor),
              color,
              params.lineWidth());
        }
      }
      if (pos != null) fadePositions.put(pos, now);
    }
    fadePositions.entrySet().removeIf(e -> e.getValue() + params.fadeTime() < now);
    if (pos == null) {
      // Live cell expired: the main box and text go away, but in-flight fades
      // play out instead of being wiped. Matches 3arthh4ck's ListenerRender.
      slidePos = null;
      return;
    }
    if (params.slide() && slidePos != null && !slidePos.equals(pos)) {
      double factor =
          Math.min(1.0, (double) (now - slideStartMs) / Math.max(1, params.slideTime()));
      if (factor >= 1.0) {
        renderBox(
            boxAt(pos, params.boxHeight()),
            params.fillAlpha(),
            params.outlineAlpha(),
            color,
            params.lineWidth());
      } else {
        renderBox(
            lerpBox(boxAt(slidePos, params.boxHeight()), boxAt(pos, params.boxHeight()), factor),
            params.fillAlpha(),
            params.outlineAlpha(),
            color,
            params.lineWidth());
      }
    } else {
      renderBox(
          boxAt(pos, params.boxHeight()),
          params.fillAlpha(),
          params.outlineAlpha(),
          color,
          params.lineWidth());
    }
    if (params.damageText() && renderText != null) {
      Gizmos.billboardTextOverBlock(renderText, pos, 0, 0xFFFFFFFF, 0.5f).setAlwaysOnTop();
    }
  }

  private static void renderBox(
      AABB box, int fillAlpha, int outlineAlpha, int color, float lineWidth) {
    GizmoStyle style =
        GizmoStyle.strokeAndFill(
            ARGB.color(outlineAlpha, color), lineWidth, ARGB.color(fillAlpha, color));
    Gizmos.cuboid(box, style).setAlwaysOnTop();
  }

  private static AABB boxAt(BlockPos pos, double height) {
    return new AABB(
        pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + height, pos.getZ() + 1);
  }

  private static AABB lerpBox(AABB from, AABB to, double t) {
    return new AABB(
        from.minX + (to.minX - from.minX) * t,
        from.minY + (to.minY - from.minY) * t,
        from.minZ + (to.minZ - from.minZ) * t,
        from.maxX + (to.maxX - from.maxX) * t,
        from.maxY + (to.maxY - from.maxY) * t,
        from.maxZ + (to.maxZ - from.maxZ) * t);
  }
}
