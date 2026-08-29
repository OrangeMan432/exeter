package sh.orangeman.weirdpvp.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import sh.orangeman.weirdpvp.WeirdPvP;

import java.util.Comparator;

public class AutoCart extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> tntCarts = sgGeneral.add(new IntSetting.Builder()
        .name("tnt-carts")
        .description("The amount of TNT minecarts to use per cycle.")
        .defaultValue(5)
        .range(1, 64)
        .build()
    );

    private final Setting<Integer> cartsPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("carts-per-tick")
        .description("The amount of minecarts to place per tick.")
        .defaultValue(1)
        .range(1, 10)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The maximum range to place at.")
        .defaultValue(5.0)
        .range(0, 10)
        .build()
    );

    private final Setting<Boolean> pullFromInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("pull-from-inventory")
        .description("Pulls minecarts from your inventory into your hotbar.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotateRail = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate-rail")
        .description("Rotates when placing the rail.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotateMinecart = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate-minecart")
        .description("Rotates when placing the minecart.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> sendCompleteMessage = sgGeneral.add(new BoolSetting.Builder()
        .name("send-complete-message")
        .description("Sends a message when the module has finished placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> instaLight = sgGeneral.add(new BoolSetting.Builder()
        .name("insta-light")
        .description("Breaks the rail and lights the block underneath after placing, then repeats.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder()
        .name("break-delay")
        .description("The delay in ticks between breaking the rail and lighting the block underneath.")
        .defaultValue(1)
        .range(0, 20)
        .visible(instaLight::get)
        .build()
    );

    private Player target;
    private BlockPos targetPos;
    private int cartsPlaced;
    private int breakTimer;
    private boolean isLighting;
    private int lightStage; // 0 = break rail, 1 = light block underneath
    private boolean doneMessageSent;
    private boolean swapPending;

    public AutoCart() {
        super(WeirdPvP.CATEGORY, "auto-cart", "Places rails and TNT minecarts at the opponent's feet.");
    }

    @Override
    public void onActivate() {
        target = null;
        targetPos = null;
        cartsPlaced = 0;
        breakTimer = 0;
        isLighting = false;
        lightStage = 0;
        doneMessageSent = false;
        swapPending = false;
    }

    @Override
    public String getInfoString() {
        return String.format("%d", cartsPlaced);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (swapPending) {
            InvUtils.swapBack();
            swapPending = false;
        }

        // Continue the current lighting sequence
        if (isLighting) {
            tickLighting();
            return;
        }

        Player newTarget = findTarget();
        if (newTarget == null) {
            target = null;
            targetPos = null;
            return;
        }

        BlockPos newTargetPos = newTarget.blockPosition();

        // Start a fresh placement cycle if the target changed or moved
        if (target == null || target != newTarget || !newTargetPos.equals(targetPos)) {
            target = newTarget;
            targetPos = newTargetPos;
            cartsPlaced = 0;
            doneMessageSent = false;
        }

        // Place a rail if there isn't one under the target
        if (mc.level.getBlockState(targetPos).getBlock() != Blocks.RAIL) {
            cartsPlaced = 0;
            doneMessageSent = false;

            // Don't place rails if there's no TNT to put on them
            if (!hasTntCarts()) return;

            FindItemResult rail = InvUtils.findInHotbar(Items.RAIL);
            if (rail.found()) {
                BlockUtils.place(targetPos, rail, rotateRail.get(), 0, false);
            }
            return;
        }

        // Place TNT minecarts on the rail
        if (cartsPlaced < tntCarts.get()) {
            FindItemResult tntCart = findTntCart();

            if (tntCart.found()) {
                Runnable placeAction = () -> {
                    for (int i = 0; i < cartsPerTick.get() && cartsPlaced < tntCarts.get(); i++) {
                        BlockUtils.interact(
                            new BlockHitResult(Vec3.atCenterOf(targetPos.above()), Direction.UP, targetPos, false),
                            InteractionHand.MAIN_HAND,
                            false
                        );
                        cartsPlaced++;
                    }
                };

                InvUtils.swap(tntCart.slot(), true);

                if (rotateMinecart.get()) {
                    Rotations.rotate(Rotations.getYaw(targetPos), Rotations.getPitch(targetPos), 50, placeAction);
                } else {
                    placeAction.run();
                }
            }
            return;
        }

        // All carts are placed
        InvUtils.swapBack();

        if (instaLight.get()) {
            isLighting = true;
            breakTimer = 0;
            lightStage = 0;
            return;
        }

        if (!doneMessageSent) {
            doneMessageSent = true;
            if (sendCompleteMessage.get()) {
                info("Placed %d TNT minecarts.", cartsPlaced);
            }
        }
    }

    private Player findTarget() {
        return mc.level.players().stream()
            .filter(player -> player != mc.player && Friends.get().shouldAttack(player) && mc.player.distanceTo(player) <= range.get())
            .min(Comparator.comparing(player -> mc.player.distanceTo(player)))
            .orElse(null);
    }

    private boolean hasTntCarts() {
        return InvUtils.findInHotbar(Items.TNT_MINECART).found()
            || (pullFromInventory.get() && InvUtils.find(itemStack -> itemStack.getItem() == Items.TNT_MINECART, 9, 35).found());
    }

    private FindItemResult findTntCart() {
        FindItemResult tntCart = InvUtils.findInHotbar(Items.TNT_MINECART);

        if (!tntCart.found() && pullFromInventory.get()) {
            FindItemResult tntCartInInv = InvUtils.find(itemStack -> itemStack.getItem() == Items.TNT_MINECART, 9, 35);
            if (tntCartInInv.found()) {
                FindItemResult emptySlot = InvUtils.find(itemStack -> itemStack.isEmpty(), 0, 8);
                if (emptySlot.found()) {
                    InvUtils.move().from(tntCartInInv.slot()).to(emptySlot.slot());
                    tntCart = emptySlot;
                }
            }
        }

        return tntCart;
    }

    private void tickLighting() {
        if (lightStage == 0) { // Break rail
            FindItemResult pickaxe = InvUtils.findInHotbar(itemStack -> itemStack.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()));
            if (!pickaxe.found()) {
                // Can't break the rail, skip lighting and keep placing
                isLighting = false;
                cartsPlaced = 0;
                return;
            }

            InvUtils.swap(pickaxe.slot(), true);

            // Keep mining every tick until the rail is actually broken
            if (mc.level.getBlockState(targetPos).getBlock() != Blocks.AIR) {
                BlockUtils.breakBlock(targetPos, true);
                return;
            }

            swapPending = true;

            breakTimer = breakDelay.get();
            lightStage = 1;
        } else if (lightStage == 1) { // Light the block underneath
            if (breakTimer > 0) {
                breakTimer--;
                return;
            }

            FindItemResult flintAndSteel = InvUtils.findInHotbar(Items.FLINT_AND_STEEL);
            if (flintAndSteel.found()) {
                InvUtils.swap(flintAndSteel.slot(), true);
                BlockUtils.interact(
                    new BlockHitResult(Vec3.atCenterOf(targetPos), Direction.UP, targetPos.below(), false),
                    InteractionHand.MAIN_HAND,
                    true
                );
                swapPending = true;

                if (sendCompleteMessage.get()) {
                    info("Placed and lit %d TNT minecarts.", cartsPlaced);
                }
            }

            // Reset and start the next placement cycle
            isLighting = false;
            lightStage = 0;
            breakTimer = 0;
            cartsPlaced = 0;
        }
    }
}