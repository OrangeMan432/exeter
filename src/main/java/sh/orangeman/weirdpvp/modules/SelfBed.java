package sh.orangeman.weirdpvp.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import sh.orangeman.weirdpvp.WeirdPvP;

public class SelfBed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates to the correct angle when placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("The range to place beds.")
        .defaultValue(5.0)
        .range(0, 10)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Delay in ticks between placing and using the bed.")
        .defaultValue(1)
        .range(0, 20)
        .build()
    );

    private final Setting<Integer> useDelay = sgGeneral.add(new IntSetting.Builder()
        .name("use-delay")
        .description("Delay in ticks before using the placed bed.")
        .defaultValue(0)
        .range(0, 20)
        .build()
    );

    private int tickCounter;
    private int lastPlaceTick;
    private int lastUseTick;
    private int phase; // 0 = idle, 1 = placed, waiting to use, 2 = used, waiting to place again
    private boolean hasBed;
    private boolean swapPending;

    public SelfBed() {
        super(WeirdPvP.CATEGORY, "self-bed", "Places and uses a bed behind you for explosion knockback boost.");
    }

    @Override
    public void onActivate() {
        tickCounter = 0;
        lastPlaceTick = -100;
        lastUseTick = -100;
        phase = 0;
        hasBed = false;
        swapPending = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.level == null || mc.player == null || mc.player.isDeadOrDying()) return;

        if (swapPending) {
            InvUtils.swapBack();
            swapPending = false;
        }

        if (!mc.player.input.hasForwardImpulse()) return;

        tickCounter++;

        FindItemResult bed = findBed();
        if (!bed.found()) return;

        Direction facing = mc.player.getDirection().getOpposite();
        BlockPos playerPos = mc.player.blockPosition();

        BlockPos supportPos = findSupport(playerPos);
        if (supportPos == null) return;

        BlockPos footPos = supportPos.above();
        BlockPos headPos = footPos.relative(facing);

        if (!inRange(footPos)) return;

        // Check if bed exists at expected position
        boolean bedAtFoot = mc.level.getBlockState(footPos).getBlock() instanceof BedBlock;
        boolean bedAtHead = mc.level.getBlockState(headPos).getBlock() instanceof BedBlock;
        hasBed = bedAtFoot || bedAtHead;

        BlockPos usePos = bedAtFoot ? footPos : (bedAtHead ? headPos : null);

        switch (phase) {
            case 0: // No bed, place one
                if (hasBed) {
                    phase = 1;
                    return;
                }

                if (!isAirOrReplaceable(footPos) || !isAirOrReplaceable(headPos)) return;
                if (tickCounter - lastPlaceTick < placeDelay.get()) return;

                placeBed(supportPos, footPos, bed.slot());
                lastPlaceTick = tickCounter;
                phase = 1;
                break;

            case 1: // Bed placed, wait then use it
                if (!hasBed) {
                    phase = 0;
                    return;
                }

                if (tickCounter - lastPlaceTick < useDelay.get()) return;

                useBed(usePos, bed.slot());
                lastUseTick = tickCounter;
                phase = 2;
                break;

            case 2: // Bed used (should explode), wait then place next
                if (hasBed) {
                    // Bed hasn't exploded yet, wait more
                    if (tickCounter - lastUseTick >= 10) {
                        // Too long, assume it exploded client-side
                        phase = 0;
                    }
                    return;
                }

                phase = 0;
                break;
        }
    }

    private void placeBed(BlockPos supportPos, BlockPos footPos, int slot) {
        InvUtils.swap(slot, true);

        BlockHitResult placeHit = new BlockHitResult(
            Vec3.atCenterOf(supportPos).add(0, 0.5, 0),
            Direction.UP,
            supportPos,
            false
        );

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(footPos), Rotations.getPitch(footPos), 100, () -> {
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, placeHit);
            });
        } else {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, placeHit);
        }

        swapPending = true;
    }

    private void useBed(BlockPos usePos, int slot) {
        InvUtils.swap(slot, true);

        BlockHitResult useHit = new BlockHitResult(
            Vec3.atCenterOf(usePos),
            Direction.UP,
            usePos,
            false
        );

        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(usePos), Rotations.getPitch(usePos), 100, () -> {
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, useHit);
            });
        } else {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, useHit);
        }

        swapPending = true;
    }

    private BlockPos findSupport(BlockPos pos) {
        if (isSolid(pos)) return pos;
        if (isSolid(pos.below())) return pos.below();
        if (isSolid(pos.below(2))) return pos.below(2);
        return null;
    }

    private boolean isSolid(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(mc.level, pos).isEmpty();
    }

    private boolean isAirOrReplaceable(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private boolean inRange(BlockPos pos) {
        double dx = pos.getX() + 0.5 - mc.player.getX();
        double dy = pos.getY() + 0.5 - mc.player.getEyePosition().y;
        double dz = pos.getZ() + 0.5 - mc.player.getZ();
        return dx * dx + dy * dy + dz * dz <= range.get() * range.get();
    }

    private FindItemResult findBed() {
        return InvUtils.findInHotbar(stack ->
            stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof BedBlock
        );
    }
}
