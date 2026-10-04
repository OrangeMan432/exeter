package me.friendly.exeter.util;

import java.util.function.Predicate;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.logging.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class PlayerUtil {

  private static final Minecraft mc = Minecraft.getInstance();
  private static int previousSlot = -1;

  public static int findInHotbar(Predicate<ItemStack> predicate) {
    if (mc.player == null) return -1;
    Inventory inv = mc.player.getInventory();
    for (int i = 0; i < 9; i++) {
      if (predicate.test(inv.getItem(i))) {
        return i;
      }
    }
    return -1;
  }

  public static int findInInventory(Predicate<ItemStack> predicate) {
    if (mc.player == null) return -1;
    Inventory inv = mc.player.getInventory();
    for (int i = 0; i < 36; i++) {
      if (predicate.test(inv.getItem(i))) {
        return i;
      }
    }
    return -1;
  }

  public static int findBed() {
    return findInHotbar(
        stack -> {
          if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return false;
          Block block = ((BlockItem) stack.getItem()).getBlock();
          return block instanceof BedBlock;
        });
  }

  public static void swapTo(int slot) {
    if (mc.player == null || slot < 0 || slot > 8) return;
    previousSlot = mc.player.getInventory().getSelectedSlot();
    mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
  }

  public static void swapBack() {
    if (mc.player == null || previousSlot < 0 || previousSlot > 8) return;
    mc.player.connection.send(new ServerboundSetCarriedItemPacket(previousSlot));
    previousSlot = -1;
  }

  /**
   * Forces the server selection to match the client's actual selected slot. Silent swaps move only
   * the server selection, so any unbalanced swap (switch-back off, early return, stale slot) leaves
   * the two diverged and later placements consume from the wrong stack. Call at the end of a
   * placement sequence to heal it unconditionally.
   */
  public static void resyncSlot() {
    if (mc.player == null || mc.player.connection == null) return;
    previousSlot = -1;
    mc.player.connection.send(
        new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
  }

  public static void withRotation(double yaw, double pitch, Runnable action) {
    if (mc.player == null) return;
    float origYaw = mc.player.getYRot();
    float origPitch = mc.player.getXRot();
    mc.player.setYRot((float) yaw);
    mc.player.setXRot((float) pitch);
    action.run();
    mc.player.setYRot(origYaw);
    mc.player.setXRot(origPitch);
  }

  public static void setRotation(double yaw, double pitch) {
    if (mc.player == null) return;
    mc.player.setYRot((float) yaw);
    mc.player.setXRot((float) pitch);
  }

  public static void restoreRotation(float yaw, float pitch) {
    if (mc.player == null) return;
    mc.player.setYRot(yaw);
    mc.player.setXRot(pitch);
  }

  /**
   * Silent rotation in the style of Homovore's RotationManager. Modules engage a spoofed look with
   * {@link #setSpoofedLook} and every outgoing movement packet has its look replaced 1:1 by {@link
   * #spoofMovement}, so the server sees the spoofed facing while the client camera never moves.
   * Replacing 1:1 matters: the server kicks on two position packets in one tick, so extra movement
   * packets must never be sent alongside vanilla's own.
   */
  private static boolean spoofing = false;

  private static String spoofOwner = null;
  private static float spoofYaw;
  private static float spoofPitch;
  private static boolean swapping = false;
  private static boolean deliveredThisTick = false;
  private static int spoofDebugCount = 0;
  private static long lastSpoofSendMs = 0;

  public static void setSpoofedLook(String owner, float yaw, float pitch) {
    spoofing = true;
    spoofOwner = owner;
    spoofYaw = yaw;
    spoofPitch = pitch;
  }

  public static boolean isSpoofing() {
    return spoofing;
  }

  /** Releases this owner's spoof. Vanilla's next movement packet resyncs the server look. */
  public static void clearSpoofedLook(String owner) {
    if (!spoofing || (spoofOwner != null && !spoofOwner.equals(owner))) return;
    spoofing = false;
    spoofOwner = null;
  }

  /**
   * PacketEvent handler for outgoing movement. Cancels vanilla's packet and resends it with the
   * spoofed look, keeping exactly one movement packet on the wire.
   */
  public static void spoofMovement(PacketEvent event) {
    // Inbound reads (either side, e.g. the integrated server reading our packet over loopback)
    // must never be touched: canceling + resending those loops forever and the server would
    // never apply movement.
    if (!event.isSending()) return;
    if (!spoofing || swapping) return;
    if (event.isCanceled()) return;
    if (!(event.getPacket() instanceof ServerboundMovePlayerPacket move)) return;
    if (!move.hasRotation() && !move.hasPosition()) return;
    double x = move.getX(0.0);
    double y = move.getY(0.0);
    double z = move.getZ(0.0);
    if (!Double.isFinite(x)
        || !Double.isFinite(y)
        || !Double.isFinite(z)
        || !Float.isFinite(spoofYaw)
        || !Float.isFinite(spoofPitch)) {
      DebugLogger.get()
          .logFile(
              "Spoof",
              "dropping non-finite movement, vanilla=("
                  + x
                  + ","
                  + y
                  + ","
                  + z
                  + ") spoof=("
                  + spoofYaw
                  + ","
                  + spoofPitch
                  + ") owner="
                  + spoofOwner);
      event.setCanceled(true);
      return;
    }
    Packet<?> replacement;
    if (move.hasPosition()) {
      replacement =
          new ServerboundMovePlayerPacket.PosRot(
              x, y, z, spoofYaw, spoofPitch, move.isOnGround(), move.horizontalCollision());
    } else {
      replacement =
          new ServerboundMovePlayerPacket.Rot(
              spoofYaw, spoofPitch, move.isOnGround(), move.horizontalCollision());
    }
    event.setCanceled(true);
    swapping = true;
    try {
      if (mc.player != null && mc.player.connection != null) {
        DebugLogger.get()
            .logFile(
                "Spoof",
                event.getPacket().getClass().getSimpleName()
                    + " replaced: look=("
                    + spoofYaw
                    + ","
                    + spoofPitch
                    + ") owner="
                    + spoofOwner);
        if (spoofDebugCount < 40) {
          spoofDebugCount++;
          DebugLogger.get()
              .logFile(
                  "SpoofDbg", Thread.currentThread().getName() + " sender=" + findPacketSender());
        }
        mc.player.connection.send(replacement);
        deliveredThisTick = true;
        lastSpoofSendMs = System.currentTimeMillis();
      }
    } finally {
      swapping = false;
    }
  }

  /** Temporary: identifies who is sending movement packets at burst rates. */
  private static String findPacketSender() {
    for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
      String cls = frame.getClassName();
      if (cls.startsWith("java.")
          || cls.startsWith("jdk.")
          || cls.contains("me.friendly.exeter.util.PlayerUtil")
          || cls.contains("me.friendly.api.event")
          || cls.contains("me.friendly.exeter.mixin.MixinNetworkManager")
          || cls.contains("net.minecraft.network.Connection")
          || cls.contains("io.netty")
          || cls.contains("org.spongepowered")) {
        continue;
      }
      return cls + "." + frame.getMethodName() + ":" + frame.getLineNumber();
    }
    return "?";
  }

  /**
   * Returns and clears the per-tick delivery flag. Modules call this on tick end: if vanilla sent
   * nothing replaceable that tick (idle ticks), a rotation-only top-up is needed instead.
   */
  public static boolean consumeDeliveredFlag() {
    boolean delivered = deliveredThisTick;
    deliveredThisTick = false;
    return delivered;
  }

  /** Rotation-only spoof top-up for ticks where vanilla sent no movement packet. Never kicks. */
  public static void sendSpoofTopUp() {
    if (!spoofing || mc.player == null || mc.player.connection == null) return;
    mc.player.connection.send(
        new ServerboundMovePlayerPacket.Rot(
            spoofYaw, spoofPitch, mc.player.onGround(), mc.player.horizontalCollision));
    lastSpoofSendMs = System.currentTimeMillis();
    DebugLogger.get()
        .logFile("Spoof", "top-up Rot sent: look=(" + spoofYaw + "," + spoofPitch + ")");
  }

  /** Millis timestamp of the last spoofed-look packet sent, for placement freshness gates. */
  public static long lastSpoofSendMs() {
    return lastSpoofSendMs;
  }

  /** Survival block-interact reach enforced by the server on use-item packets. */
  public static final double INTERACT_RANGE = 4.5;

  /**
   * Clicks the surface of the block face, like vanilla. The server spawns fireworks at click +
   * face*0.15 and places blocks against the clicked face, so a block-center click ends up inside
   * solid geometry (rockets detonate embedded with all damage rays blocked).
   */
  public static void useItemOn(BlockPos pos, Direction face) {
    if (mc.player == null || mc.gameMode == null) return;
    Vec3 click =
        new Vec3(
            pos.getX() + 0.5 + face.getStepX() * 0.5,
            pos.getY() + 0.5 + face.getStepY() * 0.5,
            pos.getZ() + 0.5 + face.getStepZ() * 0.5);
    BlockHitResult hit = new BlockHitResult(click, face, pos, false);
    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
  }

  public static void swingHand() {
    if (mc.player == null) return;
    mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
  }

  /**
   * Clicks a solid neighbour when one exists so the block lands in the cell; otherwise clicks the
   * replaceable cell itself.
   */
  public static void clickPlace(BlockPos cell) {
    if (mc.player == null || mc.gameMode == null || mc.level == null) return;
    for (Direction dir : Direction.values()) {
      BlockPos neighbour = cell.relative(dir);
      var state = mc.level.getBlockState(neighbour);
      if (state.isAir() || state.canBeReplaced()) continue;
      useItemOn(neighbour, dir.getOpposite());
      return;
    }
    useItemOn(cell, Direction.UP);
  }

  /** Hotbar slot holding the given block, or -1. */
  public static int findBlock(Block block) {
    return findInHotbar(
        stack ->
            stack.getItem() instanceof BlockItem
                && ((BlockItem) stack.getItem()).getBlock() == block);
  }

  /** Yaw facing a horizontal piston direction. */
  public static float yawFor(Direction dir) {
    return switch (dir) {
      case NORTH -> 180f;
      case SOUTH -> 0f;
      case EAST -> -90f;
      case WEST -> 90f;
      default -> 0f;
    };
  }

  public static double getYaw(BlockPos pos) {
    if (mc.player == null) return 0;
    double dx = pos.getX() + 0.5 - mc.player.getX();
    double dz = pos.getZ() + 0.5 - mc.player.getZ();
    return Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
  }

  public static double getPitch(BlockPos pos) {
    if (mc.player == null) return 0;
    double dx = pos.getX() + 0.5 - mc.player.getX();
    double dy = pos.getY() + 0.5 - (mc.player.getY() + mc.player.getEyeHeight());
    double dz = pos.getZ() + 0.5 - mc.player.getZ();
    double dist = Math.sqrt(dx * dx + dz * dz);
    return -Math.toDegrees(Math.atan2(dy, dist));
  }

  public static boolean isMoving() {
    if (mc.player == null) return false;
    return mc.player.input.hasForwardImpulse()
        || mc.player.input.keyPresses.backward()
        || mc.player.input.keyPresses.left()
        || mc.player.input.keyPresses.right();
  }

  public static boolean isSolid(BlockPos pos) {
    if (mc.level == null) return false;
    BlockState state = mc.level.getBlockState(pos);
    return state.canOcclude();
  }

  public static boolean isAirOrReplaceable(BlockPos pos) {
    if (mc.level == null) return false;
    BlockState state = mc.level.getBlockState(pos);
    return state.isAir() || state.canBeReplaced();
  }

  public static boolean inRange(BlockPos pos, double range) {
    if (mc.player == null) return false;
    return mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
        <= range * range;
  }

  public static void breakBlock(BlockPos pos, Direction face) {
    if (mc.player == null || mc.gameMode == null) return;
    mc.player.connection.send(
        new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, face));
    mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
    mc.player.connection.send(
        new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, face));
  }
}
