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

public class BedAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates to the target when placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Automatically switches to bed in hotbar.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder()
        .name("switch-back")
        .description("Switches back to previous slot after placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("The range to place beds.")
        .defaultValue(5.0)
        .range(0, 10)
        .build()
    );

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The range to detect targets.")
        .defaultValue(10.0)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Delay in ticks between placing beds.")
        .defaultValue(0)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder()
        .name("break-delay")
        .description("Delay in ticks between using/breaking beds.")
        .defaultValue(1)
        .range(0, 20)
        .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swings hand when placing.")
        .defaultValue(true)
        .build()
    );

    private int tickCounter;
    private int lastPlaceTick;
    private int lastBreakTick;
    private Player target;
    private BlockPos lastBedPos;
    private boolean waitingToBreak;
    private boolean waitingToPlace;

    public BedAura() {
        super(WeirdPvP.CATEGORY, "bed-aura", "Automatically places and uses/breaks beds near targets.");
    }

    @Override
    public void onActivate() {
        tickCounter = 0;
        lastPlaceTick = -100;
        lastBreakTick = -100;
        target = null;
        lastBedPos = null;
        waitingToBreak = false;
        waitingToPlace = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null || mc.player == null || mc.player.isDeadOrDying()) return;

        tickCounter++;

        FindItemResult bed = findBed();
        if (!bed.found() && !autoSwitch.get()) return;

        target = findTarget();
        if (target == null) return;

        if (waitingToBreak) {
            if (tickCounter - lastBreakTick >= 2) {
                mc.gameMode.stopDestroyBlock();
                waitingToBreak = false;
                lastPlaceTick = tickCounter;
            }
            return;
        }

        if (waitingToPlace) {
            if (tickCounter - lastPlaceTick >= breakDelay.get()) {
                if (lastBedPos != null) {
                    boolean bedExists = mc.level.getBlockState(lastBedPos).getBlock() instanceof net.minecraft.world.level.block.BedBlock;
                    if (bedExists) {
                        useBed(lastBedPos, bed.slot());
                    }
                }
                waitingToPlace = false;
                waitingToBreak = false;
                lastBreakTick = tickCounter;
            }
            return;
        }

        if (tickCounter - lastPlaceTick < placeDelay.get()) return;

        BlockPos placePos = findBestPlacePos();
        if (placePos == null) return;

        placeBed(placePos, bed.slot());
        lastBedPos = placePos;
        lastPlaceTick = tickCounter;
        waitingToPlace = true;
    }

    private void placeBed(BlockPos pos, int slot) {
        boolean needSwitch = autoSwitch.get() && slot != mc.player.getInventory().getSelectedSlot();

        if (needSwitch) {
            InvUtils.swap(slot, true);
        }

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, () -> {
                sendPlacePacket(pos);
            });
        } else {
            sendPlacePacket(pos);
        }

        if (swingHand.get()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        if (needSwitch && switchBack.get()) {
            InvUtils.swapBack();
        }
    }

    private void sendPlacePacket(BlockPos bedPos) {
        BlockPos supportPos = bedPos.below();
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(supportPos).add(0, 0.5, 0),
            Direction.UP,
            supportPos,
            false
        );
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
    }

    private void useBed(BlockPos pos, int slot) {
        boolean needSwitch = autoSwitch.get() && slot != mc.player.getInventory().getSelectedSlot();

        if (needSwitch) {
            InvUtils.swap(slot, true);
        }

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), 100, () -> {
                sendUsePacket(pos);
            });
        } else {
            sendUsePacket(pos);
        }

        if (swingHand.get()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        if (needSwitch && switchBack.get()) {
            InvUtils.swapBack();
        }
    }

    private void sendUsePacket(BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(pos),
            Direction.UP,
            pos,
            false
        );
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
    }

    private BlockPos findBestPlacePos() {
        List<BlockPos> positions = new ArrayList<>();

        int range = (int) Math.ceil(placeRange.get());
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = mc.player.blockPosition().offset(x, y, z);
                    if (canPlaceBed(pos)) {
                        positions.add(pos);
                    }
                }
            }
        }

        return positions.stream()
            .min(Comparator.comparingDouble(pos -> pos.distSqr(target.blockPosition())))
            .orElse(null);
    }

    private boolean canPlaceBed(BlockPos pos) {
        if (!mc.level.getBlockState(pos).isAir()) return false;

        BlockPos supporting = pos.below();
        if (mc.level.getBlockState(supporting).isAir()) return false;

        Direction facing = mc.player.getDirection().getOpposite();
        BlockPos headPos = pos.relative(facing);
        if (!mc.level.getBlockState(headPos).isAir()) return false;

        if (target != null) {
            double distToTarget = pos.distSqr(target.blockPosition());
            if (distToTarget > targetRange.get() * targetRange.get()) return false;
        }

        return true;
    }

    private Player findTarget() {
        List<Player> players = new ArrayList<>();

        for (Entity entity : mc.level.players()) {
            if (entity == mc.player) continue;
            if (!entity.isAlive()) continue;

            double distance = mc.player.distanceTo(entity);
            if (distance <= targetRange.get()) {
                players.add((Player) entity);
            }
        }

        return players.stream()
            .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
            .orElse(null);
    }

    private FindItemResult findBed() {
        return InvUtils.findInHotbar(stack ->
            stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof net.minecraft.world.level.block.BedBlock
        );
    }
}
