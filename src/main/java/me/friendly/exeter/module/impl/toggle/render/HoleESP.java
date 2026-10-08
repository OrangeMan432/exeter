package me.friendly.exeter.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Hole ESP ported from Lemon's HoleESP: finds 1x1, 2x1 and 2x2 holes in range and colors them green
 * for bedrock, red for obsidian.
 */
public class HoleESP extends ToggleableModule {

  public enum RenderMode {
    OUTLINED,
    FILLED,
    BOTH
  }

  public enum ShapeMode {
    AIR,
    GROUND,
    FLAT,
    DOUBLE
  }

  private final NumberProperty<Integer> range = new NumberProperty<Integer>(5, 1, 20, "Range");
  private final NumberProperty<Integer> yRange = new NumberProperty<Integer>(5, 1, 20, "Y Range");
  private final Property<Boolean> useSingle = new Property<Boolean>(true, "1x1");
  private final Property<Boolean> useDouble = new Property<Boolean>(true, "2x1");
  private final Property<Boolean> useQuad = new Property<Boolean>(true, "2x2");
  private final EnumProperty<RenderMode> renderMode =
      new EnumProperty<RenderMode>(RenderMode.BOTH, "Render", "mode");
  private final EnumProperty<ShapeMode> shapeMode =
      new EnumProperty<ShapeMode>(ShapeMode.AIR, "Mode", "shape");
  private final Property<Boolean> hideOwn = new Property<Boolean>(false, "Hide Own");
  private final Property<Boolean> flatOwn = new Property<Boolean>(false, "Flat Own");
  private final NumberProperty<Float> lineWidth =
      new NumberProperty<Float>(2.0f, 0.5f, 10.0f, "Width");
  private final Property<Boolean> useCustomAlpha =
      new Property<Boolean>(false, "Custom Alpha", "CustomAlpha");
  private final NumberProperty<Float> fillAlpha =
      new NumberProperty<Float>(50f, 0f, 255f, "Fill Alpha", "FillAlpha");
  private final NumberProperty<Float> outlineAlpha =
      new NumberProperty<Float>(255f, 0f, 255f, "Outline Alpha", "OutlineAlpha");

  private final Map<AABB, Integer> holes = new HashMap<>();
  private final Set<BlockPos> claimed = new HashSet<>();
  private int tickCounter;

  public HoleESP() {
    super("HoleESP", new String[] {"holeesp", "holes"}, 0xFF8800, ModuleType.RENDER);
    setDescription("Highlights 1x1, 2x1 and 2x2 holes: green bedrock, red obsidian.");
    fillAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    outlineAlpha.visibleWhen(() -> useCustomAlpha.getValue());
    offerProperties(
        range,
        yRange,
        useSingle,
        useDouble,
        useQuad,
        renderMode,
        shapeMode,
        hideOwn,
        flatOwn,
        lineWidth,
        useCustomAlpha,
        fillAlpha,
        outlineAlpha);
    this.listeners.add(
        new Listener<TickEvent>("hole_esp_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });
    this.listeners.add(
        new Listener<WorldRenderEvent>("hole_esp_render") {
          @Override
          public void call(WorldRenderEvent event) {
            onRender();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    holes.clear();
    claimed.clear();
    tickCounter = 0;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    holes.clear();
    claimed.clear();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  private void onTick() {
    tickCounter++;
    if (tickCounter < 10) return;
    tickCounter = 0;
    holes.clear();
    claimed.clear();
    if (minecraft.level == null || minecraft.player == null) return;

    BlockPos center = minecraft.player.blockPosition();
    int r = range.getValue();
    int yr = yRange.getValue();
    Set<BlockPos> floors = new HashSet<>();
    for (int dx = -r; dx <= r; dx++) {
      for (int dy = -yr; dy <= yr; dy++) {
        for (int dz = -r; dz <= r; dz++) {
          BlockPos pos = center.offset(dx, dy, dz);
          if (!isHoleFloor(pos)) continue;
          floors.add(pos.immutable());
        }
      }
    }

    // Biggest first so 2x2 and 2x1 claim their cells before singles see them.
    for (BlockPos pos : new ArrayList<>(floors)) {
      if (!useQuad.getValue() || claimed.contains(pos)) continue;
      AABB quad = quadAt(pos, floors);
      if (quad != null) {
        holes.put(quad, quadColor(quad));
        claimCells(pos, 2, 2);
      }
    }
    for (BlockPos pos : new ArrayList<>(floors)) {
      if (!useDouble.getValue() || claimed.contains(pos)) continue;
      AABB pair = pairAt(pos, floors);
      if (pair != null) {
        holes.put(pair, pairColor(pair));
      }
    }
    if (useSingle.getValue()) {
      for (BlockPos pos : floors) {
        if (claimed.contains(pos)) continue;
        if (!isSingleHole(pos)) continue;
        holes.put(new AABB(pos), singleColor(pos));
      }
    }
  }

  /** Air cell with solid floor and two air above: holeable ground. */
  private boolean isHoleFloor(BlockPos pos) {
    if (!minecraft.level.getBlockState(pos).isAir()) return false;
    if (minecraft.level.getBlockState(pos.below()).isAir()) return false;
    if (!minecraft.level.getBlockState(pos.above()).isAir()) return false;
    return minecraft.level.getBlockState(pos.above(2)).isAir();
  }

  /** 1x1 with four solid walls. */
  private boolean isSingleHole(BlockPos pos) {
    for (Direction dir : Direction.Plane.HORIZONTAL) {
      if (minecraft.level.getBlockState(pos.relative(dir)).isAir()) return false;
    }
    return true;
  }

  /** 2x2 square of floors with a solid perimeter, or null. */
  private AABB quadAt(BlockPos pos, Set<BlockPos> floors) {
    BlockPos east = pos.east();
    BlockPos south = pos.south();
    BlockPos corner = pos.east().south();
    if (!floors.contains(east) || !floors.contains(south) || !floors.contains(corner)) return null;
    for (BlockPos cell : List.of(pos, east, south, corner)) {
      for (Direction dir : Direction.Plane.HORIZONTAL) {
        BlockPos wall = cell.relative(dir);
        if (wall.getX() >= pos.getX()
            && wall.getX() <= pos.getX() + 1
            && wall.getZ() >= pos.getZ()
            && wall.getZ() <= pos.getZ() + 1) continue;
        if (minecraft.level.getBlockState(wall).isAir()) return null;
      }
    }
    return new AABB(
        pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 2, pos.getY() + 1, pos.getZ() + 2);
  }

  /** 1x2 pair extending east or south, or null. */
  private AABB pairAt(BlockPos pos, Set<BlockPos> floors) {
    if (floors.contains(pos.east()) && pairWallsOk(pos, pos.east())) {
      claimCells(pos, 2, 1);
      return new AABB(
          pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 2, pos.getY() + 1, pos.getZ() + 1);
    }
    if (floors.contains(pos.south()) && pairWallsOk(pos, pos.south())) {
      claimCells(pos, 1, 2);
      return new AABB(
          pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 2);
    }
    return null;
  }

  private boolean pairWallsOk(BlockPos a, BlockPos b) {
    int minX = Math.min(a.getX(), b.getX());
    int maxX = Math.max(a.getX(), b.getX());
    int minZ = Math.min(a.getZ(), b.getZ());
    int maxZ = Math.max(a.getZ(), b.getZ());
    int y = a.getY();
    // Long sides plus both short ends must be solid; corners may be open.
    if (minX == maxX) {
      if (isAir(new BlockPos(minX, y, minZ - 1)) || isAir(new BlockPos(minX, y, maxZ + 1))) {
        return false;
      }
      for (int z = minZ; z <= maxZ; z++) {
        if (isAir(new BlockPos(minX - 1, y, z)) || isAir(new BlockPos(maxX + 1, y, z))) {
          return false;
        }
      }
      return true;
    }
    if (isAir(new BlockPos(minX - 1, y, minZ)) || isAir(new BlockPos(maxX + 1, y, minZ))) {
      return false;
    }
    for (int x = minX; x <= maxX; x++) {
      if (isAir(new BlockPos(x, y, minZ - 1)) || isAir(new BlockPos(x, y, maxZ + 1))) {
        return false;
      }
    }
    return true;
  }

  private boolean isAir(BlockPos pos) {
    return minecraft.level.getBlockState(pos).isAir();
  }

  private void claimCells(BlockPos origin, int sx, int sz) {
    for (int x = 0; x < sx; x++) {
      for (int z = 0; z < sz; z++) {
        claimed.add(new BlockPos(origin.getX() + x, origin.getY(), origin.getZ() + z));
      }
    }
  }

  /** Green when floor and walls are all bedrock, red otherwise. */
  private boolean allBedrock(AABB box) {
    int minX = (int) Math.floor(box.minX) - 1;
    int maxX = (int) Math.floor(box.maxX - 1e-7) + 1;
    int minY = (int) Math.floor(box.minY) - 1;
    int maxY = (int) Math.floor(box.maxY - 1e-7);
    int minZ = (int) Math.floor(box.minZ) - 1;
    int maxZ = (int) Math.floor(box.maxZ - 1e-7) + 1;
    for (int x = minX; x <= maxX; x++) {
      for (int y = minY; y <= maxY; y++) {
        for (int z = minZ; z <= maxZ; z++) {
          BlockState state = minecraft.level.getBlockState(new BlockPos(x, y, z));
          if (state.isAir()) continue;
          if (!state.is(Blocks.BEDROCK)) return false;
        }
      }
    }
    return true;
  }

  private int singleColor(BlockPos pos) {
    return allBedrock(new AABB(pos)) ? 0xFF00FF00 : 0xFFFF0000;
  }

  private int pairColor(AABB pair) {
    return allBedrock(pair) ? 0xFF00FF00 : 0xFFFF0000;
  }

  private int quadColor(AABB quad) {
    return allBedrock(quad) ? 0xFF00FF00 : 0xFFFF0000;
  }

  private void onRender() {
    if (holes.isEmpty() || minecraft.player == null) return;
    boolean filled =
        renderMode.getValue() == RenderMode.FILLED || renderMode.getValue() == RenderMode.BOTH;
    boolean outlined =
        renderMode.getValue() == RenderMode.OUTLINED || renderMode.getValue() == RenderMode.BOTH;
    AABB playerBox = minecraft.player.getBoundingBox();

    for (var entry : holes.entrySet()) {
      AABB hole = entry.getKey();
      int color = entry.getValue();
      if (hideOwn.getValue() && hole.intersects(playerBox)) continue;
      boolean flat =
          shapeMode.getValue() == ShapeMode.FLAT
              || (flatOwn.getValue() && hole.intersects(playerBox));
      if (flat) {
        renderFlat(hole, color);
        continue;
      }
      AABB box = shapeBox(hole);
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
      Gizmos.cuboid(box, style).setAlwaysOnTop();
    }
  }

  /** True flat: bottom face fill plus bottom perimeter only, like Lemon. */
  private void renderFlat(AABB hole, int color) {
    boolean filled =
        renderMode.getValue() == RenderMode.FILLED || renderMode.getValue() == RenderMode.BOTH;
    boolean outlined =
        renderMode.getValue() == RenderMode.OUTLINED || renderMode.getValue() == RenderMode.BOTH;
    int fillAlphaVal =
        useCustomAlpha.getValue()
            ? Math.round(fillAlpha.getValue())
            : me.friendly.exeter.render.EspRenderManager.getGlobalFillAlpha();
    int outlineAlphaVal =
        useCustomAlpha.getValue()
            ? Math.round(outlineAlpha.getValue())
            : me.friendly.exeter.render.EspRenderManager.getGlobalOutlineAlpha();
    double y = hole.minY;
    net.minecraft.world.phys.Vec3 p1 = new net.minecraft.world.phys.Vec3(hole.minX, y, hole.minZ);
    net.minecraft.world.phys.Vec3 p2 = new net.minecraft.world.phys.Vec3(hole.maxX, y, hole.minZ);
    net.minecraft.world.phys.Vec3 p3 = new net.minecraft.world.phys.Vec3(hole.maxX, y, hole.maxZ);
    net.minecraft.world.phys.Vec3 p4 = new net.minecraft.world.phys.Vec3(hole.minX, y, hole.maxZ);
    if (filled) {
      int fillColor = ARGB.color(fillAlphaVal, color);
      var fill = GizmoStyle.fill(fillColor);
      Gizmos.rect(p1, p2, p3, p4, fill).setAlwaysOnTop();
      Gizmos.rect(p1, p4, p3, p2, fill).setAlwaysOnTop();
    }
    if (outlined) {
      int outlineColor = ARGB.color(outlineAlphaVal, color);
      float w = lineWidth.getValue();
      Gizmos.line(p1, p2, outlineColor, w).setAlwaysOnTop();
      Gizmos.line(p2, p3, outlineColor, w).setAlwaysOnTop();
      Gizmos.line(p3, p4, outlineColor, w).setAlwaysOnTop();
      Gizmos.line(p4, p1, outlineColor, w).setAlwaysOnTop();
    }
  }

  /** Air shows the hole, Ground the floor below it, Flat a slab, Double two blocks tall. */
  private AABB shapeBox(AABB hole) {
    return switch (shapeMode.getValue()) {
      case GROUND -> new AABB(hole.minX, hole.minY - 1, hole.minZ, hole.maxX, hole.minY, hole.maxZ);
      case FLAT ->
          new AABB(hole.minX, hole.minY, hole.minZ, hole.maxX, hole.minY + 0.15, hole.maxZ);
      case DOUBLE -> new AABB(hole.minX, hole.minY, hole.minZ, hole.maxX, hole.maxY + 1, hole.maxZ);
      default -> hole;
    };
  }
}
