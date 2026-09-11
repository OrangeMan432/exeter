package me.larp.client.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;

public class PistonPush extends ToggleableModule {
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> debug = new Property<Boolean>(false, "Debug");
  private final NumberProperty<Integer> range = new NumberProperty<Integer>(6, 0, 10, "Range");
  private final NumberProperty<Integer> placeDelay =
      new NumberProperty<Integer>(0, 0, 20, "Place Delay");

  private int tickCounter;
  private int lastPlaceTick;
  private Player target;

  public PistonPush() {
    super("PistonPush", new String[] {"pistonpush", "piston-push"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Uses pistons to push players into dangerous positions.");
    offerProperties(rotate, swingHand, debug, range, placeDelay);
    this.listeners.add(
        new Listener<TickEvent>("piston_push_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
    lastPlaceTick = -100;
    target = null;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying())
      return;

    tickCounter++;

    if (tickCounter - lastPlaceTick < placeDelay.getValue()) return;

    int pistonSlot = findBlock(Blocks.PISTON);
    if (pistonSlot == -1) {
      pistonSlot = findBlock(Blocks.STICKY_PISTON);
    }
    if (pistonSlot == -1) return;

    int redstoneSlot = findBlock(Blocks.REDSTONE_BLOCK);
    if (redstoneSlot == -1) {
      redstoneSlot = findBlock(Blocks.REDSTONE_TORCH);
    }
    if (redstoneSlot == -1) return;

    target = findTarget();
    if (target == null) return;

    PistonPos pos = findPistonPos(target);
    if (pos == null) return;

    placePiston(pos, pistonSlot, redstoneSlot);
    lastPlaceTick = tickCounter;
  }

  private void placePiston(PistonPos pos, int pistonSlot, int redstoneSlot) {
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();

    if (rotate.getValue()) {
      float pistonYaw = getYawForDirection(pos.facing);
      PlayerUtil.setRotation(pistonYaw, 0f);
    }

    boolean needPiston = pistonSlot != minecraft.player.getInventory().getSelectedSlot();
    if (needPiston) {
      PlayerUtil.swapTo(pistonSlot);
    }
    clickNeighbor(pos.pistonPos);
    if (swingHand.getValue()) PlayerUtil.swingHand();
    if (needPiston) {
      PlayerUtil.swapBack();
    }

    if (rotate.getValue()) {
      PlayerUtil.setRotation(
          PlayerUtil.getYaw(pos.redstonePos), PlayerUtil.getPitch(pos.redstonePos));
    }

    boolean needRedstone = redstoneSlot != minecraft.player.getInventory().getSelectedSlot();
    if (needRedstone) {
      PlayerUtil.swapTo(redstoneSlot);
    }
    clickNeighbor(pos.redstonePos);
    if (swingHand.getValue()) PlayerUtil.swingHand();
    if (needRedstone) {
      PlayerUtil.swapBack();
    }

    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }

    if (debug.getValue()) {
      me.larp.client.logging.Logger.getLogger()
          .printToChat(
              String.format(
                  "PistonPush -> %s piston %s redstone %s",
                  target.getName().getString(),
                  pos.pistonPos.toShortString(),
                  pos.redstonePos.toShortString()));
    }
  }

  /** Clicks a solid neighbor face so the server accepts the placement. */
  private void clickNeighbor(BlockPos pos) {
    for (Direction dir : Direction.values()) {
      BlockPos neighbor = pos.relative(dir);
      if (PlayerUtil.isSolid(neighbor)) {
        PlayerUtil.useItemOn(neighbor, dir.getOpposite());
        return;
      }
    }
    // Fallback: legacy top-face click on the block below.
    PlayerUtil.useItemOn(pos.below(), Direction.UP);
  }

  private float getYawForDirection(Direction facing) {
    return switch (facing) {
      case NORTH -> 180f;
      case SOUTH -> 0f;
      case EAST -> -90f;
      case WEST -> 90f;
      default -> 0f;
    };
  }

  private PistonPos findPistonPos(Player target) {
    List<PistonPos> positions = new ArrayList<>();

    for (Direction facing : Direction.Plane.HORIZONTAL) {
      BlockPos targetPos = target.blockPosition();
      BlockPos pistonPos = targetPos.relative(facing.getOpposite());

      if (!canPlacePiston(pistonPos)) continue;

      for (Direction redstoneDir : Direction.Plane.HORIZONTAL) {
        BlockPos redstonePos = pistonPos.relative(redstoneDir);

        if (canPlaceRedstone(redstonePos)) {
          positions.add(new PistonPos(pistonPos, redstonePos, facing));
        }
      }

      BlockPos redstoneAbove = pistonPos.above();
      if (canPlaceRedstone(redstoneAbove)) {
        positions.add(new PistonPos(pistonPos, redstoneAbove, facing));
      }

      BlockPos redstoneBelow = pistonPos.below();
      if (canPlaceRedstone(redstoneBelow)) {
        positions.add(new PistonPos(pistonPos, redstoneBelow, facing));
      }
    }

    return positions.stream()
        .min(
            Comparator.comparingDouble(
                p ->
                    minecraft.player.distanceToSqr(
                        p.pistonPos.getX() + 0.5,
                        p.pistonPos.getY() + 0.5,
                        p.pistonPos.getZ() + 0.5)))
        .orElse(null);
  }

  private boolean canPlacePiston(BlockPos pos) {
    if (!PlayerUtil.isAirOrReplaceable(pos)) return false;
    return PlayerUtil.inRange(pos, range.getValue());
  }

  private boolean canPlaceRedstone(BlockPos pos) {
    if (!minecraft.level.getBlockState(pos).isAir()) return false;

    return PlayerUtil.inRange(pos, range.getValue());
  }

  private Player findTarget() {
    List<Player> players = new ArrayList<>();

    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player) continue;
      if (!entity.isAlive()) continue;

      double distance = minecraft.player.distanceTo(entity);
      if (distance <= range.getValue()) {
        players.add((Player) entity);
      }
    }

    return players.stream()
        .min(Comparator.comparingDouble(p -> minecraft.player.distanceTo(p)))
        .orElse(null);
  }

  private int findBlock(net.minecraft.world.level.block.Block block) {
    return PlayerUtil.findInHotbar(
        stack ->
            stack.getItem() instanceof BlockItem
                && ((BlockItem) stack.getItem()).getBlock() == block);
  }

  private static class PistonPos {
    final BlockPos pistonPos;
    final BlockPos redstonePos;
    final Direction facing;

    PistonPos(BlockPos pistonPos, BlockPos redstonePos, Direction facing) {
      this.pistonPos = pistonPos;
      this.redstonePos = redstonePos;
      this.facing = facing;
    }
  }
}
