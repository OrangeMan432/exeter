package me.larp.client.module.impl.toggle.combat;

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
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Logic-based surround breaker: finds the weakest side of the target's
 * feet ring (skips bedrock, prefers already-damaged progress via fresh
 * START packets) and packet-mines it with the best pickaxe.
 */
public class CityMiner extends ToggleableModule {

  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(8.0, 1.0, 12.0, "Target Range");
  private final NumberProperty<Double> mineRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Mine Range");
  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(0, 0, 20, "Delay");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  private BlockPos current;
  private int tickCounter;
  private int lastMineTick = -100;

  public CityMiner() {
    super("CityMiner", new String[] {"cityminer", "city-miner", "surroundbreaker"}, 0xFF0000,
        ModuleType.COMBAT);
    setDescription("Mines open enemy surrounds and traps.");
    offerProperties(targetRange, mineRange, delay, rotate, autoSwitch, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("cityminer_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            CityMiner.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    current = null;
    tickCounter = 0;
    lastMineTick = -100;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    tickCounter++;
    if (tickCounter - lastMineTick < delay.getValue()) return;

    Player target = findTarget();
    if (target == null) {
      current = null;
      return;
    }

    // Stick to one block until it breaks: progress never resets.
    if (current == null || !isBreakable(current)
        || current.distSqr(target.blockPosition()) > 8) {
      current = pickBlock(target);
    }
    if (current == null) return;
    if (!PlayerUtil.inRange(current, mineRange.getValue())) return;

    swapToBestTool(minecraft.level.getBlockState(current));

    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(current), PlayerUtil.getPitch(current));
    }
    Direction face = bestFace(current);
    minecraft.getConnection().send(
        new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, current, face));
    minecraft.getConnection().send(
        new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, current, face));
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    setTag("CityMiner [" + minecraft.level.getBlockState(current).getBlock().getName().getString() + "]");
    lastMineTick = tickCounter;
  }

  private BlockPos pickBlock(Player target) {
    BlockPos feet = target.blockPosition();
    BlockPos[] ring = {
      feet.north(), feet.south(), feet.east(), feet.west(),
      feet.above().north(), feet.above().south(), feet.above().east(), feet.above().west()
    };
    BlockPos best = null;
    double bestDist = Double.MAX_VALUE;
    for (BlockPos pos : ring) {
      if (!isBreakable(pos)) continue;
      if (!PlayerUtil.inRange(pos, mineRange.getValue())) continue;
      double d = pos.distSqr(minecraft.player.blockPosition());
      if (d < bestDist) {
        bestDist = d;
        best = pos;
      }
    }
    return best;
  }

  private boolean isBreakable(BlockPos pos) {
    BlockState state = minecraft.level.getBlockState(pos);
    if (state.isAir()) return false;
    return state.getBlock() != Blocks.BEDROCK
        && state.getBlock() != Blocks.REINFORCED_DEEPSLATE
        && state.getBlock() != Blocks.END_PORTAL_FRAME;
  }

  private Player findTarget() {
    Player best = null;
    double bestDist = targetRange.getValue();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player || !entity.isAlive()) continue;
      double d = minecraft.player.distanceTo(entity);
      if (d < bestDist) {
        bestDist = d;
        best = (Player) entity;
      }
    }
    return best;
  }

  private Direction bestFace(BlockPos pos) {
    for (Direction dir : Direction.values()) {
      if (!PlayerUtil.isSolid(pos.relative(dir))) {
        return dir.getOpposite();
      }
    }
    return Direction.UP;
  }

  private void swapToBestTool(BlockState state) {
    if (!autoSwitch.getValue()) return;
    int best = -1;
    float bestSpeed = 1.0f;
    for (int i = 0; i < 9; i++) {
      ItemStack stack = minecraft.player.getInventory().getItem(i);
      if (stack.isEmpty()) continue;
      float speed = stack.getDestroySpeed(state);
      if (speed > bestSpeed) {
        bestSpeed = speed;
        best = i;
      }
    }
    if (best != -1 && best != minecraft.player.getInventory().getSelectedSlot()) {
      PlayerUtil.swapTo(best);
    }
  }
}
