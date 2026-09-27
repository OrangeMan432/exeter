package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.render.EspRenderManager;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/**
 * Pushes PLAYERS with pistons. Picks the nearest targetable player, puts a piston behind them
 * facing into them, and powers it with a redstone block. The piston faces the player because
 * placement rotates to look along the push axis (pistons face opposite the look direction).
 */
public class PistonPush extends ToggleableModule {
  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(6.0, 0.0, 12.0, "Target Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 0.0, 10.0, "Place Range");
  private final NumberProperty<Integer> attemptDelay =
      new NumberProperty<Integer>(10, 0, 40, "Attempt Delay");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> switchBack = new Property<Boolean>(true, "Switch Back");
  private final Property<Boolean> showEsp = new Property<Boolean>(true, "Show ESP", "ESP");

  private int tickCounter;
  private int lastAttemptTick;
  private BlockPos verifyPos;
  private Direction verifyFacing;
  private String lastNullReason;
  private String lastPushKey;
  private String lastVerifyKey;

  public PistonPush() {
    super("PistonPush", new String[] {"pistonpush", "piston-push"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Pushes players with pistons.");
    offerProperties(
        targetRange, placeRange, attemptDelay, rotate, swingHand, autoSwitch, switchBack, showEsp);
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
    lastAttemptTick = -100;
    verifyPos = null;
    lastNullReason = null;
    lastPushKey = null;
    lastVerifyKey = null;
    DebugLogger.get()
        .log(
            getLabel(),
            DebugLogger.Level.INFO,
            "enabled: targetRange="
                + targetRange.getValue()
                + " placeRange="
                + placeRange.getValue());
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "disabled");
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying())
      return;

    tickCounter++;

    if (verifyPos != null) {
      var state = minecraft.level.getBlockState(verifyPos);
      String result;
      DebugLogger.Level level;
      if (state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON)) {
        Direction actual = state.getValue(BlockStateProperties.FACING);
        result = "piston at " + verifyPos + " faces " + actual + " (intended " + verifyFacing + ")";
        level = actual == verifyFacing ? DebugLogger.Level.INFO : DebugLogger.Level.WARN;
      } else {
        result =
            "no piston at " + verifyPos + ", placement failed (state=" + state.getBlock() + ")";
        level = DebugLogger.Level.WARN;
      }
      // File-only detail, and chat only when the outcome changes.
      DebugLogger.get().logFile(getLabel(), "verify: " + result);
      if (!result.equals(lastVerifyKey)) {
        lastVerifyKey = result;
        DebugLogger.get().log(getLabel(), level, "verify: " + result);
      }
      verifyPos = null;
    }

    if (tickCounter - lastAttemptTick < attemptDelay.getValue()) return;

    int pistonSlot = findBlock(Blocks.PISTON);
    if (pistonSlot == -1) pistonSlot = findBlock(Blocks.STICKY_PISTON);
    int redstoneSlot = findBlock(Blocks.REDSTONE_BLOCK);
    if (pistonSlot == -1 || redstoneSlot == -1) {
      DebugLogger.get()
          .log(
              getLabel(),
              DebugLogger.Level.WARN,
              "missing materials: pistonSlot=" + pistonSlot + " redstoneSlot=" + redstoneSlot);
      return;
    }

    Player target = findTarget();
    if (target == null) return;

    PushSetup setup = findSetup(target);
    if (setup == null) {
      DebugLogger.get()
          .log(
              getLabel(),
              DebugLogger.Level.INFO,
              "no piston position for " + target.getName().getString());
      String reason = diagnose(target);
      if (!reason.equals(lastNullReason)) {
        lastNullReason = reason;
        DebugLogger.get()
            .logFile(
                getLabel(),
                "no setup for "
                    + target.getName().getString()
                    + " at "
                    + target.blockPosition()
                    + " (dist="
                    + String.format("%.1f", minecraft.player.distanceTo(target))
                    + "): "
                    + reason);
      }
      lastAttemptTick = tickCounter;
      return;
    }
    lastNullReason = null;

    if (alreadyPowered(setup)) return;

    // Per-attempt detail goes to the file only; chat/notifications only on a new setup.
    String pushKey =
        target.getName().getString() + setup.pistonPos + setup.facing + setup.redstonePos;
    String detail =
        "pushing "
            + target.getName().getString()
            + ": piston "
            + setup.pistonPos
            + " facing "
            + setup.facing
            + " redstone "
            + setup.redstonePos
            + " playerLook=("
            + minecraft.player.getYRot()
            + ", "
            + minecraft.player.getXRot()
            + ") sentLook=("
            + yawFor(setup.facing.getOpposite())
            + ", 0.0)";
    DebugLogger.get().logFile(getLabel(), detail);
    if (!pushKey.equals(lastPushKey)) {
      lastPushKey = pushKey;
      DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, detail);
    }
    placePush(setup, pistonSlot, redstoneSlot);
    lastAttemptTick = tickCounter;
  }

  private void placePush(PushSetup setup, int pistonSlot, int redstoneSlot) {
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    boolean needSwitch =
        autoSwitch.getValue() && pistonSlot != minecraft.player.getInventory().getSelectedSlot();

    if (needSwitch) PlayerUtil.swapTo(pistonSlot);
    // Pistons face opposite the look direction, so look away from the push to face the player.
    // The look is also sent to the server: placement facing is computed server-side from the
    // last sent rotation, so a client-only rotation would face the piston the wrong way.
    float pistonYaw = yawFor(setup.facing.getOpposite());
    if (rotate.getValue()) {
      PlayerUtil.setRotation(pistonYaw, 0f);
      sendLook(pistonYaw, 0f);
    }
    clickPlace(setup.pistonPos);
    if (swingHand.getValue()) PlayerUtil.swingHand();
    if (needSwitch && switchBack.getValue()) PlayerUtil.swapBack();

    needSwitch =
        autoSwitch.getValue() && redstoneSlot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) PlayerUtil.swapTo(redstoneSlot);
    if (rotate.getValue()) {
      PlayerUtil.setRotation(
          PlayerUtil.getYaw(setup.redstonePos), PlayerUtil.getPitch(setup.redstonePos));
      sendLook(
          (float) PlayerUtil.getYaw(setup.redstonePos),
          (float) PlayerUtil.getPitch(setup.redstonePos));
    }
    clickPlace(setup.redstonePos);
    if (swingHand.getValue()) PlayerUtil.swingHand();
    if (needSwitch && switchBack.getValue()) PlayerUtil.swapBack();

    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
      sendLook(yaw, pitch);
    }

    verifyPos = setup.pistonPos;
    verifyFacing = setup.facing;

    if (showEsp.getValue()) {
      EspRenderManager.getInstance().addBoxEsp(new AABB(setup.pistonPos), 3.0f, true, true, -1, -1);
      EspRenderManager.getInstance()
          .addBoxEsp(new AABB(setup.redstonePos), 3.0f, true, true, -1, -1);
    }
  }

  /**
   * Clicks a solid neighbour when one exists so the block lands in the cell; otherwise clicks the
   * replaceable cell itself.
   */
  private void clickPlace(BlockPos cell) {
    for (Direction dir : Direction.values()) {
      BlockPos neighbour = cell.relative(dir);
      var state = minecraft.level.getBlockState(neighbour);
      if (state.isAir() || state.canBeReplaced()) continue;
      PlayerUtil.useItemOn(neighbour, dir.getOpposite());
      return;
    }
    PlayerUtil.useItemOn(cell, Direction.UP);
  }

  private void sendLook(float yaw, float pitch) {
    if (minecraft.player == null || minecraft.player.connection == null) return;
    minecraft.player.connection.send(
        new ServerboundMovePlayerPacket.Rot(yaw, pitch, minecraft.player.onGround(), false));
  }

  private PushSetup findSetup(Player target) {
    BlockPos feet = target.blockPosition();
    List<PushSetup> options = new ArrayList<>();
    // Level 0 pushes the legs, level 1 pushes the head: in a 1-deep hole the feet cells are
    // the hole walls, so only the head-level setup can push the player out.
    for (int level = 0; level <= 1; level++) {
      BlockPos cell = feet.above(level);
      for (Direction push : Direction.Plane.HORIZONTAL) {
        BlockPos pistonPos = cell.relative(push.getOpposite());
        BlockPos dest = cell.relative(push);
        if (!PlayerUtil.isAirOrReplaceable(pistonPos)) continue;
        if (!PlayerUtil.isAirOrReplaceable(dest)) continue;
        if (!PlayerUtil.inRange(pistonPos, placeRange.getValue())) continue;
        BlockPos redstonePos = findRedstoneSpot(pistonPos);
        if (redstonePos == null) continue;
        options.add(new PushSetup(pistonPos, redstonePos, push));
      }
    }
    return options.stream()
        .min(
            Comparator.comparingDouble(
                s ->
                    minecraft.player.distanceToSqr(
                        s.pistonPos.getX() + 0.5,
                        s.pistonPos.getY() + 0.5,
                        s.pistonPos.getZ() + 0.5)))
        .orElse(null);
  }

  /** Explains why no setup exists, so a failed hole push can be diagnosed from the log file. */
  private String diagnose(Player target) {
    BlockPos feet = target.blockPosition();
    StringBuilder out = new StringBuilder();
    for (int level = 0; level <= 1; level++) {
      BlockPos cell = feet.above(level);
      for (Direction push : Direction.Plane.HORIZONTAL) {
        BlockPos pistonPos = cell.relative(push.getOpposite());
        BlockPos dest = cell.relative(push);
        String why;
        if (!PlayerUtil.isAirOrReplaceable(pistonPos)) {
          why = "piston@" + pistonPos + "=" + blockName(pistonPos);
        } else if (!PlayerUtil.isAirOrReplaceable(dest)) {
          why = "dest@" + dest + "=" + blockName(dest);
        } else if (!PlayerUtil.inRange(pistonPos, placeRange.getValue())) {
          why = "piston out of range";
        } else if (findRedstoneSpot(pistonPos) == null) {
          why = "no redstone spot";
        } else {
          why = "ok";
        }
        out.append("L").append(level).append(push).append(":").append(why).append(" ");
      }
    }
    return out.toString().trim();
  }

  private String blockName(BlockPos pos) {
    return String.valueOf(minecraft.level.getBlockState(pos).getBlock());
  }

  private BlockPos findRedstoneSpot(BlockPos pistonPos) {
    // Reuse an existing power block first so we never stack duplicates.
    for (Direction dir : Direction.values()) {
      BlockPos pos = pistonPos.relative(dir);
      if (minecraft.level.getBlockState(pos).is(Blocks.REDSTONE_BLOCK)) return pos;
    }
    BlockPos above = pistonPos.above();
    if (PlayerUtil.isAirOrReplaceable(above) && PlayerUtil.inRange(above, placeRange.getValue()))
      return above;
    for (Direction dir : Direction.Plane.HORIZONTAL) {
      BlockPos pos = pistonPos.relative(dir);
      if (PlayerUtil.isAirOrReplaceable(pos) && PlayerUtil.inRange(pos, placeRange.getValue()))
        return pos;
    }
    BlockPos below = pistonPos.below();
    if (PlayerUtil.isAirOrReplaceable(below) && PlayerUtil.inRange(below, placeRange.getValue()))
      return below;
    return null;
  }

  private boolean alreadyPowered(PushSetup setup) {
    if (minecraft.level.getBlockState(setup.pistonPos).is(Blocks.PISTON)
        || minecraft.level.getBlockState(setup.pistonPos).is(Blocks.STICKY_PISTON)) {
      for (Direction dir : Direction.values()) {
        if (minecraft.level.getBlockState(setup.pistonPos.relative(dir)).is(Blocks.REDSTONE_BLOCK))
          return true;
      }
    }
    return false;
  }

  private static float yawFor(Direction dir) {
    return switch (dir) {
      case NORTH -> 180f;
      case SOUTH -> 0f;
      case EAST -> -90f;
      case WEST -> 90f;
      default -> 0f;
    };
  }

  private Player findTarget() {
    List<Player> players = new ArrayList<>();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player) continue;
      if (!entity.isAlive()) continue;
      if (!Exeter.getInstance().getFriendManager().isTargetable(entity.getName().getString())) {
        continue;
      }
      if (minecraft.player.distanceTo(entity) <= targetRange.getValue()) {
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

  private static class PushSetup {
    final BlockPos pistonPos;
    final BlockPos redstonePos;
    final Direction facing;

    PushSetup(BlockPos pistonPos, BlockPos redstonePos, Direction facing) {
      this.pistonPos = pistonPos;
      this.redstonePos = redstonePos;
      this.facing = facing;
    }
  }
}
