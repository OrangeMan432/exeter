package sh.orangeman.weirdpvp.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import sh.orangeman.weirdpvp.WeirdPvP;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AntiHoleCamper extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates when placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("The range to detect targets.")
        .defaultValue(6)
        .range(0, 10)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Delay in ticks between placing.")
        .defaultValue(0)
        .range(0, 20)
        .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swings hand when placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("debug")
        .description("Prints debug messages.")
        .defaultValue(false)
        .build()
    );

    private int tickCounter;
    private int lastPlaceTick;
    private Player target;
    private PistonPos currentPiston;
    private boolean swapPending;

    public AntiHoleCamper() {
        super(WeirdPvP.CATEGORY, "anti-hole-camper", "Pushes enemies out of holes using pistons and redstone.");
    }

    @Override
    public void onActivate() {
        tickCounter = 0;
        lastPlaceTick = -100;
        target = null;
        currentPiston = null;
        swapPending = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null || mc.player == null || mc.player.isDeadOrDying()) return;

        if (swapPending) {
            InvUtils.swapBack();
            swapPending = false;
        }

        tickCounter++;

        if (tickCounter - lastPlaceTick < placeDelay.get()) return;

        FindItemResult piston = findItem(Blocks.PISTON);
        if (!piston.found()) {
            piston = findItem(Blocks.STICKY_PISTON);
        }
        if (!piston.found()) return;

        FindItemResult redstone = findItem(Blocks.REDSTONE_BLOCK);
        if (!redstone.found()) {
            redstone = findItem(Blocks.REDSTONE_TORCH);
        }
        if (!redstone.found()) return;

        target = findTarget();
        if (target == null) return;

        PistonPos pos = findPistonPos(target);
        if (pos == null) return;

        placePiston(pos, piston.slot());
        lastPlaceTick = tickCounter;
    }

    private void placePiston(PistonPos pos, int pistonSlot) {
        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(pos.pistonPos), Rotations.getPitch(pos.pistonPos), 100, () -> {
                InvUtils.swap(pistonSlot, true);
                sendPlacePacket(pos.pistonPos);
                swapPending = true;
            });
        } else {
            InvUtils.swap(pistonSlot, true);
            sendPlacePacket(pos.pistonPos);
            swapPending = true;
        }

        if (swingHand.get()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        FindItemResult redstone = findItem(Blocks.REDSTONE_BLOCK);
        if (!redstone.found()) {
            redstone = findItem(Blocks.REDSTONE_TORCH);
        }

        if (redstone.found()) {
            int redstoneSlot = redstone.slot();
            if (rotate.get()) {
                Rotations.rotate(Rotations.getYaw(pos.redstonePos), Rotations.getPitch(pos.redstonePos), 100, () -> {
                    InvUtils.swap(redstoneSlot, true);
                    sendPlacePacket(pos.redstonePos);
                    swapPending = true;
                });
            } else {
                InvUtils.swap(redstoneSlot, true);
                sendPlacePacket(pos.redstonePos);
                swapPending = true;
            }
        }
    }

    private void sendPlacePacket(BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(pos),
            Direction.UP,
            pos,
            false
        );
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
    }

    private PistonPos findPistonPos(Player target) {
        List<PistonPos> positions = new ArrayList<>();

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos targetPos = target.blockPosition();
            BlockPos pistonPos = targetPos.relative(facing.getOpposite());

            if (!canPlacePiston(pistonPos, facing)) continue;

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
            .min(Comparator.comparingDouble(p -> mc.player.distanceToSqr(Vec3.atCenterOf(p.pistonPos))))
            .orElse(null);
    }

    private boolean canPlacePiston(BlockPos pos, Direction facing) {
        if (!mc.level.getBlockState(pos).isAir()) return false;

        if (!BlockUtils.canPlace(pos)) return false;

        BlockPos pushPos = pos.relative(facing.getOpposite());
        if (!mc.level.getBlockState(pushPos).isAir()) return false;

        return mc.player.distanceToSqr(Vec3.atCenterOf(pos)) <= range.get() * range.get();
    }

    private boolean canPlaceRedstone(BlockPos pos) {
        if (!mc.level.getBlockState(pos).isAir()) return false;

        return mc.player.distanceToSqr(Vec3.atCenterOf(pos)) <= range.get() * range.get();
    }

    private Player findTarget() {
        List<Player> players = new ArrayList<>();

        for (Entity entity : mc.level.players()) {
            if (entity == mc.player) continue;
            if (!entity.isAlive()) continue;

            double distance = mc.player.distanceTo(entity);
            if (distance <= range.get()) {
                players.add((Player) entity);
            }
        }

        return players.stream()
            .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
            .orElse(null);
    }

    private FindItemResult findItem(net.minecraft.world.level.block.Block block) {
        return InvUtils.findInHotbar(stack ->
            stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() == block
        );
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
