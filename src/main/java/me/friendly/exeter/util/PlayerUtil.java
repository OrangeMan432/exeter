package me.friendly.exeter.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

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
        return findInHotbar(stack -> {
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

    public static void useItemOn(BlockPos pos, Direction face) {
        if (mc.player == null || mc.gameMode == null) return;
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(pos), face, pos, false
        );
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
    }

    public static void swingHand() {
        if (mc.player == null) return;
        mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
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
        return mc.player.input.hasForwardImpulse();
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
        return mc.player.distanceToSqr(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        ) <= range * range;
    }
}
