package me.larp.client.module.impl.toggle.world;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Shoreline-pattern Nuker: breaks every breakable block in range, optional
 * flatten pass for a clean floor.
 */
public class Nuker extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(5.0, 1.0, 7.0, "Range");
  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(0, 0, 10, "Delay");
  private final Property<Boolean> flatten =
      new Property<Boolean>(false, "Flatten");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  private int tickCounter;

  public Nuker() {
    super("Nuker", new String[] {"nuker", "nuke"}, 0x0000FF, ModuleType.WORLD);
    setDescription("Destroys all breakable blocks around you.");
    offerProperties(range, delay, flatten, rotate, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("nuker_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Nuker.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (tickCounter++ < delay.getValue()) return;
    tickCounter = 0;

    BlockPos target = findTarget();
    if (target == null) return;

    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(target), PlayerUtil.getPitch(target));
    }
    PlayerUtil.breakBlock(target, Direction.UP);
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    setTag("Nuker [" + minecraft.level.getBlockState(target).getBlock().getName().getString() + "]");
  }

  private BlockPos findTarget() {
    BlockPos origin = minecraft.player.blockPosition();
    int r = (int) Math.ceil(range.getValue());
    BlockPos best = null;
    double bestDist = Double.MAX_VALUE;
    for (int x = -r; x <= r; x++) {
      for (int y = -r; y <= r; y++) {
        for (int z = -r; z <= r; z++) {
          BlockPos pos = origin.offset(x, y, z);
          BlockState state = minecraft.level.getBlockState(pos);
          if (state.isAir()) continue;
          if (!minecraft.level.getFluidState(pos).isEmpty()) continue;
          if (!isBreakable(state.getBlock())) continue;
          if (flatten.getValue() && pos.getY() < origin.getY() - 1) continue;
          double dx = pos.getX() + 0.5 - minecraft.player.getX();
          double dy = pos.getY() + 0.5 - minecraft.player.getY();
          double dz = pos.getZ() + 0.5 - minecraft.player.getZ();
          double distSq = dx * dx + dy * dy + dz * dz;
          if (distSq > range.getValue() * range.getValue()) continue;
          if (distSq < bestDist) {
            bestDist = distSq;
            best = pos;
          }
        }
      }
    }
    return best;
  }

  private static boolean isBreakable(Block block) {
    return block != Blocks.BEDROCK
        && block != Blocks.REINFORCED_DEEPSLATE
        && block != Blocks.END_PORTAL_FRAME
        && block != Blocks.END_PORTAL
        && block != Blocks.NETHER_PORTAL
        && block != Blocks.COMMAND_BLOCK
        && block != Blocks.BARRIER;
  }
}
