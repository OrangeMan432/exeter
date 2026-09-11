package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Lemon-pattern HolePush: when an enemy stands next to a hole, seats a piston
 * to shove them in and powers it with a torch.
 */
public class HolePush extends ToggleableModule {

  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(8.0, 1.0, 12.0, "Target Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Place Range");
  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(4, 0, 20, "Delay");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  private int tickCounter;
  private int lastActionTick = -100;

  public HolePush() {
    super("HolePush", new String[] {"holepush", "hole-push"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Pistons enemies into nearby holes.");
    offerProperties(targetRange, placeRange, delay, rotate, autoSwitch, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("holepush_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            HolePush.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
    lastActionTick = -100;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    tickCounter++;
    if (tickCounter - lastActionTick < delay.getValue()) return;

    Player target = findTarget();
    if (target == null) return;
    int pistonSlot = PlayerUtil.findInHotbar(HolePush::isPiston);
    if (pistonSlot == -1) return;

    // Enemy must stand adjacent to a hole for the shove to land.
    BlockPos feet = target.blockPosition();
    for (Direction dir : Direction.Plane.HORIZONTAL) {
      BlockPos hole = feet.relative(dir);
      if (!isHole(hole)) continue;
      BlockPos pistonPos = feet.relative(dir.getOpposite());
      if (!PlayerUtil.isAirOrReplaceable(pistonPos)) continue;
      if (!PlayerUtil.isSolid(pistonPos.below())) continue;
      if (!PlayerUtil.inRange(pistonPos, placeRange.getValue())) continue;
      placePiston(pistonPos, dir, pistonSlot);
      power(pistonPos);
      lastActionTick = tickCounter;
      return;
    }
  }

  private boolean isHole(BlockPos pos) {
    if (!PlayerUtil.isAirOrReplaceable(pos)) return false;
    if (!PlayerUtil.isAirOrReplaceable(pos.above())) return false;
    if (!PlayerUtil.isSolid(pos.below())) return false;
    return true;
  }

  private Player findTarget() {
    Player best = null;
    double bestDist = targetRange.getValue();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player || !entity.isAlive()) continue;
      if (!entity.onGround()) continue;
      double d = minecraft.player.distanceTo(entity);
      if (d < bestDist) {
        bestDist = d;
        best = (Player) entity;
      }
    }
    return best;
  }

  private void placePiston(BlockPos pos, Direction pushDir, int slot) {
    swap(slot);
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(yawFor(pushDir), 0f);
    }
    clickNeighbor(pos);
    swing();
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    unswap(slot);
  }

  private void power(BlockPos pistonPos) {
    int slot = PlayerUtil.findInHotbar(
        s -> !s.isEmpty()
            && (s.getItem() == Items.REDSTONE_TORCH || s.getItem() == Items.REDSTONE_BLOCK));
    if (slot == -1) return;
    swap(slot);
    PlayerUtil.useItemOn(pistonPos, Direction.UP);
    swing();
    unswap(slot);
  }

  private void swap(int slot) {
    if (autoSwitch.getValue() && slot != minecraft.player.getInventory().getSelectedSlot()) {
      PlayerUtil.swapTo(slot);
    }
  }

  private void unswap(int slot) {
    if (autoSwitch.getValue() && slot != minecraft.player.getInventory().getSelectedSlot()) {
      PlayerUtil.swapBack();
    }
  }

  private void swing() {
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
  }

  private void clickNeighbor(BlockPos pos) {
    for (Direction dir : Direction.values()) {
      BlockPos neighbor = pos.relative(dir);
      if (PlayerUtil.isSolid(neighbor)) {
        PlayerUtil.useItemOn(neighbor, dir.getOpposite());
        return;
      }
    }
    PlayerUtil.useItemOn(pos.below(), Direction.UP);
  }

  private float yawFor(Direction facing) {
    return switch (facing) {
      case NORTH -> 180f;
      case SOUTH -> 0f;
      case EAST -> -90f;
      case WEST -> 90f;
      default -> 0f;
    };
  }

  private static boolean isPiston(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == Blocks.PISTON || item.getBlock() == Blocks.STICKY_PISTON;
  }
}
