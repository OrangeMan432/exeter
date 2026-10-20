package me.friendly.exeter.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import me.friendly.exeter.render.PlacementRender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Crosshair block highlight ported from Lemon's BlockHighlight: outlines or fills the looked-at
 * block, or just the targeted face.
 */
public class BlockHighlight extends ToggleableModule {

  public enum LookMode {
    BLOCK,
    SIDE
  }

  public enum RenderMode {
    OUTLINED,
    FILLED,
    BOTH
  }

  /** Box left behind by the block you just stopped looking at. */
  private static final class FadingBox {
    final AABB box;
    final long startMs;

    FadingBox(AABB box, long startMs) {
      this.box = box;
      this.startMs = startMs;
    }
  }

  private final List<FadingBox> fading = new ArrayList<>();
  private AABB slideFrom;
  private AABB lastBox;
  private BlockPos lastPos;
  private long slideStartMs;

  private final EnumProperty<LookMode> lookMode =
      new EnumProperty<LookMode>(LookMode.BLOCK, "Render", "look", "mode");
  private final EnumProperty<RenderMode> renderMode =
      new EnumProperty<RenderMode>(RenderMode.OUTLINED, "Type", "type");
  private final NumberProperty<Float> lineWidth =
      new NumberProperty<Float>(2.0f, 0.5f, 5.0f, "Line Width");
  private final Property<Boolean> useCustomAlpha =
      new Property<Boolean>(false, "Custom Alpha", "CustomAlpha");
  private final NumberProperty<Float> fillAlpha =
      new NumberProperty<Float>(50f, 0f, 255f, "Fill Alpha", "FillAlpha");
  private final NumberProperty<Float> outlineAlpha =
      new NumberProperty<Float>(255f, 0f, 255f, "Outline Alpha", "OutlineAlpha");
  private final Property<Boolean> fade = new Property<Boolean>(true, "Fade");
  private final NumberProperty<Integer> fadeTime =
      new NumberProperty<Integer>(1000, 0, 5000, "Fade Time");
  private final EnumProperty<PlacementRender.FadeMode> fadeMode =
      new EnumProperty<>(PlacementRender.FadeMode.ALPHA, "Fade Mode", "fademode");
  private final Property<Boolean> slide = new Property<Boolean>(false, "Slide");
  private final NumberProperty<Integer> slideTime =
      new NumberProperty<Integer>(250, 1, 1000, "Slide Time");
  private final Property<Boolean> smoothSlide = new Property<Boolean>(false, "Smooth Slide");

  public BlockHighlight() {
    super(
        "BlockHighlight", new String[] {"blockhighlight", "blockhi"}, 0xFF5555, ModuleType.RENDER);
    setDescription("Highlights the block under your crosshair.");
    fillAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    outlineAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    fadeMode.visibleWhen(() -> fade.getValue());
    fadeTime.visibleWhen(() -> fade.getValue());
    smoothSlide.visibleWhen(() -> slide.getValue());
    slideTime.visibleWhen(() -> slide.getValue());
    offerProperties(
        lookMode,
        renderMode,
        lineWidth,
        useCustomAlpha,
        fillAlpha,
        outlineAlpha,
        fade,
        fadeTime,
        fadeMode,
        slide,
        slideTime,
        smoothSlide);
    lookMode.setDescription("Whether the whole block or just the targeted face is highlighted.");
    renderMode.setDescription("Whether the highlight is filled, outlined, or both.");
    lineWidth.setDescription("Outline thickness in pixels.");
    useCustomAlpha.setDescription(
        "Use the Fill/Outline Alpha below instead of the global ESP alphas.");
    fillAlpha.setDescription("Box fill opacity, 0-255.");
    outlineAlpha.setDescription("Box outline opacity, 0-255.");
    fade.setDescription("Fade the box out where you stopped looking at it.");
    fadeTime.setDescription("How long the old box takes to fade, in milliseconds.");
    fadeMode.setDescription("Fade the old box by alpha or by shrinking it away.");
    slide.setDescription("Slide the box over to the new block instead of jumping.");
    slideTime.setDescription("How long a slide takes, in milliseconds.");
    smoothSlide.setDescription("Only restart the slide once the previous one finished.");
    this.listeners.add(
        new Listener<WorldRenderEvent>("block_highlight_render") {
          @Override
          public void call(WorldRenderEvent event) {
            onRender();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    fading.clear();
    slideFrom = null;
    lastBox = null;
    lastPos = null;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  private void onRender() {
    long now = System.currentTimeMillis();
    AABB box = null;
    BlockPos pos = null;
    if (minecraft.level != null
        && minecraft.player != null
        && minecraft.hitResult instanceof BlockHitResult hit
        && hit.getType() == HitResult.Type.BLOCK) {
      BlockPos candidate = hit.getBlockPos();
      BlockState state = minecraft.level.getBlockState(candidate);
      if (!state.isAir()) {
        AABB hitBox = state.getShape(minecraft.level, candidate).bounds().move(candidate);
        box = lookMode.getValue() == LookMode.SIDE ? faceSlab(hitBox, hit.getDirection()) : hitBox;
        pos = candidate;
      }
    }

    int fillAlphaVal =
        useCustomAlpha.getValue()
            ? Math.round(fillAlpha.getValue())
            : EspRenderManager.getGlobalFillAlpha();
    int outlineAlphaVal =
        useCustomAlpha.getValue()
            ? Math.round(outlineAlpha.getValue())
            : EspRenderManager.getGlobalOutlineAlpha();

    // The block you just left keeps rendering: fading in place, and the slide origin.
    if (pos == null || !pos.equals(lastPos)) {
      if (lastPos != null && lastBox != null) {
        if (fade.getValue()) fading.add(new FadingBox(lastBox, now));
        // Smooth Slide holds the origin until the previous slide finished, same rule as
        // PlacementRender.setRenderPos.
        if (!smoothSlide.getValue() || now - slideStartMs >= slideTime.getValue()) {
          slideFrom = lastBox;
          slideStartMs = now;
        }
      }
      lastPos = pos;
    }
    lastBox = box;
    if (pos == null) {
      slideFrom = null;
      if (fading.isEmpty()) return;
    }

    for (FadingBox stale : fading) {
      float factor =
          Math.min(
              1.0f,
              Math.max(
                  0.0f,
                  (float) (stale.startMs + fadeTime.getValue() - now)
                      / Math.max(1, fadeTime.getValue())));
      if (factor <= 0.0f) continue;
      if (fadeMode.getValue() == PlacementRender.FadeMode.SHRINK) {
        double height = (stale.box.maxY - stale.box.minY) * factor;
        if (height <= 0.001) continue;
        renderBox(
            new AABB(
                stale.box.minX,
                stale.box.minY,
                stale.box.minZ,
                stale.box.maxX,
                stale.box.minY + height,
                stale.box.maxZ),
            fillAlphaVal,
            outlineAlphaVal);
      } else {
        renderBox(
            stale.box, Math.round(fillAlphaVal * factor), Math.round(outlineAlphaVal * factor));
      }
    }
    fading.removeIf(f -> f.startMs + fadeTime.getValue() < now);

    if (box == null) return;
    if (slide.getValue() && slideFrom != null) {
      double factor =
          Math.min(1.0, (double) (now - slideStartMs) / Math.max(1, slideTime.getValue()));
      if (factor < 1.0) {
        renderBox(lerpBox(slideFrom, box, factor), fillAlphaVal, outlineAlphaVal);
        return;
      }
    }
    renderBox(box, fillAlphaVal, outlineAlphaVal);
  }

  private void renderBox(AABB box, int fillAlphaVal, int outlineAlphaVal) {
    boolean filled =
        renderMode.getValue() == RenderMode.FILLED || renderMode.getValue() == RenderMode.BOTH;
    boolean outlined =
        renderMode.getValue() == RenderMode.OUTLINED || renderMode.getValue() == RenderMode.BOTH;
    int color = EspRenderManager.getClientColor();
    int fillColor = ARGB.color(fillAlphaVal, color);
    int outlineColor = ARGB.color(outlineAlphaVal, color);

    GizmoStyle style;
    if (filled && outlined) {
      style = GizmoStyle.strokeAndFill(outlineColor, lineWidth.getValue(), fillColor);
    } else if (filled) {
      style = GizmoStyle.fill(fillColor);
    } else {
      style = GizmoStyle.stroke(outlineColor, lineWidth.getValue());
    }
    Gizmos.cuboid(box, style).setAlwaysOnTop();
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

  /** Thin slab hugging the targeted face, for Side mode. */
  private static AABB faceSlab(AABB box, Direction face) {
    double inset = 0.02;
    return switch (face) {
      case DOWN -> new AABB(box.minX, box.minY, box.minZ, box.maxX, box.minY + inset, box.maxZ);
      case UP -> new AABB(box.minX, box.maxY - inset, box.minZ, box.maxX, box.maxY, box.maxZ);
      case NORTH -> new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ + inset);
      case SOUTH -> new AABB(box.minX, box.minY, box.maxZ - inset, box.maxX, box.maxY, box.maxZ);
      case WEST -> new AABB(box.minX, box.minY, box.minZ, box.minX + inset, box.maxY, box.maxZ);
      case EAST -> new AABB(box.maxX - inset, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    };
  }
}
