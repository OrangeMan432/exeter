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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * BlackOut-pattern PistonCrystal: seats a piston behind the target, drops a
 * crystal between piston and target, powers with a torch, breaks on contact.
 * Staged with per-stage delays like the original.
 */
public class PistonCrystal extends ToggleableModule {

  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(8.0, 1.0, 12.0, "Target Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Place Range");
  private final NumberProperty<Integer> stageDelay =
      new NumberProperty<Integer>(2, 0, 20, "Stage Delay");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> pauseEat =
      new Property<Boolean>(false, "Pause On Eat");

  private int tickCounter;
  private int lastStageTick;
  private int stage;
  private BlockPos pistonPos;
  private BlockPos crystalPos;
  private Direction pushDir;

  public PistonCrystal() {
    super("PistonCrystal", new String[] {"pistoncrystal", "piston-crystal"}, 0xFF0000,
        ModuleType.COMBAT);
    setDescription("Pushes crystals into targets with pistons.");
    offerProperties(
        targetRange, placeRange, stageDelay, rotate, autoSwitch, swingHand, pauseEat);
    this.listeners.add(
        new Listener<TickEvent>("pistoncrystal_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            PistonCrystal.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    reset();
  }

  private void reset() {
    tickCounter = 0;
    lastStageTick = -100;
    stage = 0;
    pistonPos = null;
    crystalPos = null;
    pushDir = null;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) {
      reset();
      return;
    }
    if (pauseEat.getValue() && minecraft.player.isUsingItem()) return;
    tickCounter++;

    Player target = findTarget();
    if (target == null) {
      reset();
      return;
    }

    // Always break live crystals touching the target first.
    for (Entity entity : minecraft.level.entitiesForRendering()) {
      if (!(entity instanceof EndCrystal crystal) || !crystal.isAlive()) continue;
      if (minecraft.player.distanceTo(crystal) > placeRange.getValue()) continue;
      if (crystal.blockPosition().distSqr(target.blockPosition()) > 4) continue;
      attackCrystal(crystal);
      return;
    }

    if (stage == 0) {
      if (!findLayout(target)) return;
      placePiston();
      advance();
      return;
    }
    if (tickCounter - lastStageTick < stageDelay.getValue()) return;

    if (stage == 1) {
      if (!stillValid()) {
        reset();
        return;
      }
      placeCrystal();
      advance();
    } else if (stage == 2) {
      if (!stillValid()) {
        reset();
        return;
      }
      power();
      advance();
    } else {
      // Stage 3: keep attacking until the layout breaks, then restart.
      if (!stillValid()) {
        reset();
      }
    }
  }

  private boolean findLayout(Player target) {
    BlockPos feet = target.blockPosition();
    for (Direction dir : Direction.Plane.HORIZONTAL) {
      BlockPos piston = feet.relative(dir, 2);
      BlockPos crystal = feet.relative(dir, 1);
      if (!PlayerUtil.isAirOrReplaceable(piston)) continue;
      if (!PlayerUtil.isSolid(piston.below())) continue;
      if (!PlayerUtil.isAirOrReplaceable(crystal)) continue;
      if (!PlayerUtil.isAirOrReplaceable(crystal.above())) continue;
      net.minecraft.world.level.block.state.BlockState below =
          minecraft.level.getBlockState(crystal.below());
      if (below.getBlock() != Blocks.OBSIDIAN && below.getBlock() != Blocks.BEDROCK) continue;
      if (!PlayerUtil.inRange(piston, placeRange.getValue())) continue;
      if (!PlayerUtil.inRange(crystal, placeRange.getValue())) continue;
      pistonPos = piston;
      crystalPos = crystal;
      // Piston must face the target: opposite of the outward direction.
      pushDir = dir.getOpposite();
      return true;
    }
    return false;
  }

  private boolean stillValid() {
    if (pistonPos == null || crystalPos == null) return false;
    return minecraft.level.getBlockState(pistonPos).getBlock() == Blocks.PISTON
        || minecraft.level.getBlockState(pistonPos).getBlock() == Blocks.STICKY_PISTON
        || minecraft.level.getBlockState(pistonPos).getBlock() == Blocks.MOVING_PISTON;
  }

  private void placePiston() {
    int slot = PlayerUtil.findInHotbar(PistonCrystal::isPiston);
    if (slot == -1) return;
    swap(slot);
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(yawFor(pushDir), 0f);
    }
    clickNeighbor(pistonPos);
    swing();
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    unswap(slot);
  }

  private void placeCrystal() {
    int slot = PlayerUtil.findInHotbar(
        s -> !s.isEmpty() && s.getItem() == Items.END_CRYSTAL);
    if (slot == -1) return;
    swap(slot);
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(crystalPos), PlayerUtil.getPitch(crystalPos));
    }
    PlayerUtil.useItemOn(crystalPos.below(), Direction.UP);
    swing();
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    unswap(slot);
  }

  private void power() {
    // Redstone torch on top of the piston: piston top face is solid support.
    int slot = PlayerUtil.findInHotbar(
        s -> !s.isEmpty() && (s.getItem() == Items.REDSTONE_TORCH || s.getItem() == Items.REDSTONE_BLOCK));
    if (slot == -1) return;
    swap(slot);
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(pistonPos.above()), 0f);
    }
    PlayerUtil.useItemOn(pistonPos, Direction.UP);
    swing();
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    unswap(slot);
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

  private void attackCrystal(EndCrystal crystal) {
    minecraft.gameMode.attack(minecraft.player, crystal);
    if (swingHand.getValue()) {
      minecraft.player.swing(InteractionHand.MAIN_HAND);
    }
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

  private void advance() {
    stage++;
    lastStageTick = tickCounter;
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
