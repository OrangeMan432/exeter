package me.friendly.exeter.module.impl.toggle.render;

import java.util.HashSet;
import java.util.Set;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Void ESP ported from Lemon's VoidESP: highlights floor columns with no bedrock below them
 * (fall-through spots), active only under the configured Y outside the End.
 */
public class VoidESP extends ToggleableModule {

  public enum RenderMode {
    OUTLINED,
    FILLED,
    BOTH
  }

  public enum ShapeMode {
    BOX,
    FLAT
  }

  public enum FloorMode {
    MODERN,
    LEGACY
  }

  private final NumberProperty<Integer> distance =
      new NumberProperty<Integer>(10, 1, 40, "Distance");
  private final NumberProperty<Integer> activateY =
      new NumberProperty<Integer>(20, 0, 256, "Activate Y");
  private final EnumProperty<RenderMode> renderMode =
      new EnumProperty<RenderMode>(RenderMode.BOTH, "Render", "mode");
  private final EnumProperty<ShapeMode> shapeMode =
      new EnumProperty<ShapeMode>(ShapeMode.FLAT, "Mode", "shape");
  private final EnumProperty<FloorMode> floorMode =
      new EnumProperty<FloorMode>(FloorMode.MODERN, "Floor", "floor");
  private final NumberProperty<Float> lineWidth =
      new NumberProperty<Float>(2.0f, 0.5f, 10.0f, "Width");
  private final Property<Boolean> useCustomAlpha =
      new Property<Boolean>(false, "Custom Alpha", "CustomAlpha");
  private final NumberProperty<Float> fillAlpha =
      new NumberProperty<Float>(50f, 0f, 255f, "Fill Alpha", "FillAlpha");
  private final NumberProperty<Float> outlineAlpha =
      new NumberProperty<Float>(255f, 0f, 255f, "Outline Alpha", "OutlineAlpha");

  private final Set<BlockPos> voidHoles = new HashSet<>();
  private int tickCounter;

  public VoidESP() {
    super("VoidESP", new String[] {"voidesp", "void"}, 0xFFFF55, ModuleType.RENDER);
    setDescription("Highlights floor columns with no bedrock below them.");
    fillAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    outlineAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    offerProperties(
        distance,
        activateY,
        renderMode,
        shapeMode,
        floorMode,
        lineWidth,
        useCustomAlpha,
        fillAlpha,
        outlineAlpha);
    this.listeners.add(
        new Listener<TickEvent>("void_esp_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });
    this.listeners.add(
        new Listener<WorldRenderEvent>("void_esp_render") {
          @Override
          public void call(WorldRenderEvent event) {
            onRender();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    voidHoles.clear();
    tickCounter = 0;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    voidHoles.clear();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  private void onTick() {
    tickCounter++;
    if (tickCounter < 10) return;
    tickCounter = 0;
    voidHoles.clear();
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.level.dimension() == net.minecraft.world.level.Level.END) return;
    if (minecraft.player.blockPosition().getY() > activateY.getValue()) return;

    int dist = distance.getValue();
    BlockPos center = minecraft.player.blockPosition();
    // Modern worlds bottom out at minY; pre-1.18-style floors sit at y=0.
    int floorY = floorMode.getValue() == FloorMode.LEGACY ? 0 : minecraft.level.getMinY();
    for (int dx = -dist; dx <= dist; dx++) {
      for (int dz = -dist; dz <= dist; dz++) {
        if (dx * dx + dz * dz > dist * dist) continue;
        BlockPos floor = new BlockPos(center.getX() + dx, floorY, center.getZ() + dz);
        if (minecraft.level.getBlockState(floor).is(Blocks.BEDROCK)) continue;
        if (isAnyBedrock(floor)) continue;
        voidHoles.add(floor.immutable());
      }
    }
  }

  /** No bedrock at or just above the floor: nothing stops a fall there. */
  private boolean isAnyBedrock(BlockPos floor) {
    for (int dy = 0; dy <= 2; dy++) {
      if (minecraft.level.getBlockState(floor.above(dy)).is(Blocks.BEDROCK)) return true;
    }
    return false;
  }

  private void onRender() {
    if (voidHoles.isEmpty()) return;
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.level.dimension() == net.minecraft.world.level.Level.END) return;
    if (minecraft.player.blockPosition().getY() > activateY.getValue()) return;
    boolean filled =
        renderMode.getValue() == RenderMode.FILLED || renderMode.getValue() == RenderMode.BOTH;
    boolean outlined =
        renderMode.getValue() == RenderMode.OUTLINED || renderMode.getValue() == RenderMode.BOTH;
    int color = 0xFFFFFF00;
    int fillColor =
        ARGB.color(
            useCustomAlpha.getValue()
                ? Math.round(fillAlpha.getValue())
                : EspRenderManager.getGlobalFillAlpha(),
            color);
    int outlineColor =
        ARGB.color(
            useCustomAlpha.getValue()
                ? Math.round(outlineAlpha.getValue())
                : EspRenderManager.getGlobalOutlineAlpha(),
            color);
    GizmoStyle style;
    if (filled && outlined) {
      style = GizmoStyle.strokeAndFill(outlineColor, lineWidth.getValue(), fillColor);
    } else if (filled) {
      style = GizmoStyle.fill(fillColor);
    } else {
      style = GizmoStyle.stroke(outlineColor, lineWidth.getValue());
    }

    for (BlockPos pos : voidHoles) {
      AABB box =
          shapeMode.getValue() == ShapeMode.FLAT
              ? new AABB(
                  pos.getX(),
                  pos.getY(),
                  pos.getZ(),
                  pos.getX() + 1,
                  pos.getY() + 0.15,
                  pos.getZ() + 1)
              : new AABB(pos);
      Gizmos.cuboid(box, style).setAlwaysOnTop();
    }
  }
}
