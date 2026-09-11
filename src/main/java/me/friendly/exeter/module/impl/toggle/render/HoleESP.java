package me.friendly.exeter.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.exeter.events.RenderWorldEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.util.PlayerUtil;
import me.friendly.exeter.util.Render3D;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/** Draws outlines around safe holes in range. Nicotine-pattern 3D rendering. */
public class HoleESP extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(12.0, 2.0, 32.0, "Range");
  private final NumberProperty<Double> lineWidth =
      new NumberProperty<Double>(2.0, 1.0, 5.0, "Line Width");
  private final NumberProperty<Integer> maxHoles =
      new NumberProperty<Integer>(16, 1, 64, "Max Holes");

  public HoleESP() {
    super("HoleESP", new String[] {"holeesp", "hole-esp"}, 0xFF00FF, ModuleType.RENDER);
    setDescription("Highlights safe holes around you.");
    offerProperties(range, lineWidth, maxHoles);
    this.listeners.add(
        new Listener<RenderWorldEvent>("holeesp_render") {
          @Override
          public void call(RenderWorldEvent event) {
            HoleESP.this.onRender(event);
          }
        });
  }

  private void onRender(RenderWorldEvent event) {
    if (minecraft.level == null || minecraft.player == null) return;
    List<BlockPos> holes = findHoles();
    for (BlockPos pos : holes) {
      Render3D.drawBoxOutline(
          event.getSubmitNodeStorage(),
          event.getCamera(),
          event.getMatrixStack(),
          new AABB(pos),
          0xFF00FFFF,
          lineWidth.getValue().floatValue());
    }
  }

  private List<BlockPos> findHoles() {
    List<BlockPos> out = new ArrayList<>();
    BlockPos origin = minecraft.player.blockPosition();
    int r = (int) Math.ceil(range.getValue());
    for (int x = -r; x <= r; x++) {
      for (int y = -3; y <= 2; y++) {
        for (int z = -r; z <= r; z++) {
          if (out.size() >= maxHoles.getValue()) break;
          BlockPos pos = origin.offset(x, y, z);
          if (!isHole(pos)) continue;
          double dx = pos.getX() + 0.5 - minecraft.player.getX();
          double dy = pos.getY() + 0.5 - minecraft.player.getY();
          double dz = pos.getZ() + 0.5 - minecraft.player.getZ();
          if (dx * dx + dy * dy + dz * dz > range.getValue() * range.getValue()) continue;
          out.add(pos);
        }
      }
    }
    out.sort(Comparator.comparingDouble(p -> p.distSqr(origin)));
    return out;
  }

  private boolean isHole(BlockPos pos) {
    if (!PlayerUtil.isAirOrReplaceable(pos)) return false;
    if (!PlayerUtil.isAirOrReplaceable(pos.above())) return false;
    if (!PlayerUtil.isSolid(pos.below())) return false;
    return PlayerUtil.isSolid(pos.north())
        && PlayerUtil.isSolid(pos.south())
        && PlayerUtil.isSolid(pos.east())
        && PlayerUtil.isSolid(pos.west());
  }
}
