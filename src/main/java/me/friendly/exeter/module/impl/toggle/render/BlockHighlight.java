package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
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

  public BlockHighlight() {
    super(
        "BlockHighlight", new String[] {"blockhighlight", "blockhi"}, 0xFF5555, ModuleType.RENDER);
    setDescription("Highlights the block under your crosshair.");
    fillAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    outlineAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    offerProperties(lookMode, renderMode, lineWidth, useCustomAlpha, fillAlpha, outlineAlpha);
    lookMode.setDescription("Whether the whole block or just the targeted face is highlighted.");
    renderMode.setDescription("Whether the highlight is filled, outlined, or both.");
    lineWidth.setDescription("Outline thickness in pixels.");
    useCustomAlpha.setDescription(
        "Use the Fill/Outline Alpha below instead of the global ESP alphas.");
    fillAlpha.setDescription("Box fill opacity, 0-255.");
    outlineAlpha.setDescription("Box outline opacity, 0-255.");
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
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  private void onRender() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (!(minecraft.hitResult instanceof BlockHitResult hit)) return;
    if (hit.getType() != HitResult.Type.BLOCK) return;
    BlockPos pos = hit.getBlockPos();
    BlockState state = minecraft.level.getBlockState(pos);
    if (state.isAir()) return;
    AABB box = state.getShape(minecraft.level, pos).bounds().move(pos);

    AABB renderBox = box;
    if (lookMode.getValue() == LookMode.SIDE) {
      renderBox = faceSlab(box, hit.getDirection());
    }

    boolean filled =
        renderMode.getValue() == RenderMode.FILLED || renderMode.getValue() == RenderMode.BOTH;
    boolean outlined =
        renderMode.getValue() == RenderMode.OUTLINED || renderMode.getValue() == RenderMode.BOTH;
    int color = EspRenderManager.getClientColor();
    int fillAlphaVal =
        useCustomAlpha.getValue()
            ? Math.round(fillAlpha.getValue())
            : EspRenderManager.getGlobalFillAlpha();
    int outlineAlphaVal =
        useCustomAlpha.getValue()
            ? Math.round(outlineAlpha.getValue())
            : EspRenderManager.getGlobalOutlineAlpha();
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
    Gizmos.cuboid(renderBox, style).setAlwaysOnTop();
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
