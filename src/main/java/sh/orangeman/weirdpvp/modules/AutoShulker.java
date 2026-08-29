package sh.orangeman.weirdpvp.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import sh.orangeman.weirdpvp.WeirdPvP;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoShulker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> once = sgGeneral.add(new BoolSetting.Builder()
        .name("once")
        .description("Only places and opens one shulker box, then disables.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> emptySlots = sgGeneral.add(new IntSetting.Builder()
        .name("empty-slots")
        .description("The minimum amount of empty slots required to place a new shulker box.")
        .defaultValue(6)
        .range(1, 36)
        .visible(() -> !once.get())
        .build()
    );

    private final Setting<Boolean> disableAfterDeath = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-after-death")
        .description("Disables the module when you die.")
        .defaultValue(true)
        .visible(() -> !once.get())
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The horizontal range to place shulker boxes in.")
        .defaultValue(5.0)
        .range(0, 10)
        .build()
    );

    private final Setting<Double> yRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("y-range")
        .description("The vertical range to place shulker boxes in.")
        .defaultValue(5.0)
        .range(0, 10)
        .build()
    );

    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("Positions farther than this from the target get penalized.")
        .defaultValue(8.0)
        .range(0, 16)
        .build()
    );

    private final Setting<Integer> tickDelay = sgGeneral.add(new IntSetting.Builder()
        .name("tick-delay")
        .description("Delay in ticks between actions.")
        .defaultValue(5)
        .range(0, 10)
        .build()
    );

    private final Setting<Integer> openDelay = sgGeneral.add(new IntSetting.Builder()
        .name("open-delay")
        .description("Delay in ticks before the shulker box is opened.")
        .defaultValue(5)
        .range(0, 10)
        .build()
    );

    private final Setting<Boolean> inventory = sgGeneral.add(new BoolSetting.Builder()
        .name("inventory")
        .description("Moves shulker boxes from the inventory into the hotbar.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> slot = sgGeneral.add(new IntSetting.Builder()
        .name("slot")
        .description("The hotbar slot to move shulker boxes to.")
        .defaultValue(1)
        .range(1, 9)
        .build()
    );

    private final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("packet-place")
        .description("Places the shulker box with packets instead of the normal game mode.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> placeSwing = sgGeneral.add(new BoolSetting.Builder()
        .name("place-swing")
        .description("Swings your arm when placing the shulker box.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> packetSwing = sgGeneral.add(new BoolSetting.Builder()
        .name("packet-swing")
        .description("Sends the swing animation with a packet.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> packetSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("packet-switch")
        .description("Switches the held item with a packet instead of changing the hotbar slot.")
        .defaultValue(true)
        .build()
    );

    private int delayTimeTicks;
    private BlockPos playerPos;
    private ShulkerPos blockAim;
    private final List<BlockPos> list = new ArrayList<>();
    private int slotIndex;
    private boolean swapped;
    private int tick;

    public AutoShulker() {
        super(WeirdPvP.CATEGORY, "auto-shulker", "Places shulker boxes near you and opens them automatically.");
    }

    @Override
    public void onActivate() {
        blockAim = null;
        slotIndex = -1;
        swapped = false;
        tick = 0;
        delayTimeTicks = 0;
        checkPos();
    }

    @Override
    public void onDeactivate() {
        list.clear();
        blockAim = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        if (!once.get() && disableAfterDeath.get() && mc.player.isDeadOrDying()) {
            toggle();
            return;
        }

        if (tick++ >= openDelay.get()) {
            if (blockAim != null && inRange(blockAim.pos) && !isAir(blockAim.pos) && !canReplace(blockAim.pos)) {
                openBlock();
            }
            tick = 0;
        }

        if (mc.gui.screen() instanceof ShulkerBoxScreen) {
            if (once.get()) {
                toggle();
            }
            blockAim = null;
        } else if (delayTimeTicks++ >= tickDelay.get()) {
            delayTimeTicks = 0;
            if ((slotIndex = getShulkerSlot()) != -1) {
                if (!once.get() && getEmptyCounts() < emptySlots.get()) {
                    checkPos();
                } else if (blockAim == null) {
                    initValues();
                }

                if (blockAim == null) {
                    if (once.get()) {
                        toggle();
                    }
                } else if (!inRange(blockAim.pos)) {
                    blockAim = null;
                } else {
                    if (slotIndex > 8 && !swapped) {
                        if (!inventory.get()) {
                            return;
                        }

                        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slotIndex, slot.get() - 1, ContainerInput.SWAP, mc.player);
                        swapped = true;
                        if (tickDelay.get() != 0) {
                            return;
                        }
                    }

                    if (!isAir(blockAim.pos) && !canReplace(blockAim.pos)) {
                        openBlock();
                    } else {
                        switchTo(slotIndex, this::placeBlock);
                        if (tickDelay.get() == 0) {
                            openBlock();
                        }
                    }
                }
            }
        }
    }

    private void switchTo(int slot, Runnable runnable) {
        if (slot >= 0 && slot != mc.player.getInventory().getSelectedSlot()) {
            if (slot < 9) {
                int oldSlot = mc.player.getInventory().getSelectedSlot();
                InvUtils.swap(slot, packetSwitch.get());
                runnable.run();
                if (packetSwitch.get()) {
                    InvUtils.swapBack();
                } else {
                    mc.player.getInventory().setSelectedSlot(oldSlot);
                }
            }
        } else {
            runnable.run();
        }
    }

    private int getShulkerSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock) {
                return i;
            }
        }
        return -1;
    }

    private int getEmptyCounts() {
        int count = 0;
        for (int i = 0; i <= 35; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private boolean isPos2(BlockPos pos1, BlockPos pos2) {
        return pos1 != null && pos2 != null && pos1.equals(pos2);
    }

    private void initValues() {
        List<BlockPos> blocks = getSphere(mc.player.getEyePosition(), range.get() + 1.0, yRange.get() + 1.0);
        blocks.removeIf(p -> list.contains(p));

        List<ShulkerPos> posList = new ArrayList<>();
        for (BlockPos pos : blocks) {
            Direction facing = getFacing(pos);
            if (facing != null) {
                BlockPos neighbour = pos.relative(facing);
                Direction opposite = facing.getOpposite();
                Vec3 hitVec = Vec3.atCenterOf(neighbour).add(Vec3.atLowerCornerOf(opposite.getUnitVec3i()).scale(0.5));
                if (inRange(hitVec)) {
                    posList.add(new ShulkerPos(pos, facing, neighbour, opposite, hitVec));
                }
            }
        }

        Player target = getNearestPlayer(12.0);
        if (target == null) {
            blockAim = posList.stream().min(Comparator.comparing(p -> p.getRange(mc.player))).orElse(null);
        } else {
            blockAim = posList.stream().min(Comparator.comparing(p -> getWeight(p, target))).orElse(null);
        }

        if (blockAim != null) {
            list.add(blockAim.pos);
        }
    }

    private double getWeight(ShulkerPos pos, Player target) {
        double range = pos.getRange(target);
        if (range >= targetRange.get()) {
            int y = 256 - pos.pos.getY();
            range += y * 100;
        }
        return range;
    }

    private boolean intersectsWithEntity(BlockPos pos) {
        return mc.level.getEntities((Entity) null, new AABB(pos)).stream().anyMatch(entity -> !(entity instanceof ItemEntity));
    }

    private Direction getFacing(BlockPos pos) {
        if (!intersectsWithEntity(pos)
            && (canReplace(pos) || mc.level.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock)
            && mc.level.getBlockState(pos.above()).isAir()) {
            for (Direction facing : Direction.values()) {
                BlockPos neighbour = pos.relative(facing);
                if (canBeClicked(neighbour) && mc.level.getBlockState(neighbour.below()).isAir()) {
                    return facing;
                }
            }
        }
        return null;
    }

    private boolean inRange(Vec3 vec) {
        double x = vec.x - mc.player.getX();
        double z = vec.z - mc.player.getZ();
        double y = vec.y - mc.player.getEyePosition().y;
        double add = Math.sqrt(y * y) / 2.0;
        return x * x + z * z <= (range.get() - add) * (range.get() - add) && y * y <= yRange.get() * yRange.get();
    }

    private boolean inRange(BlockPos pos) {
        double x = pos.getX() + 0.5 - mc.player.getX();
        double z = pos.getZ() + 0.5 - mc.player.getZ();
        double y = pos.getY() + 0.5 - mc.player.getEyePosition().y;
        double add = Math.sqrt(y * y) / 2.0;
        return x * x + z * z <= (range.get() - add) * (range.get() - add) && y * y <= yRange.get() * yRange.get();
    }

    private void checkPos() {
        if (!isPos2(mc.player.blockPosition(), playerPos)) {
            list.clear();
            playerPos = mc.player.blockPosition();
        }
    }

    private void placeBlock() {
        boolean sneak = false;
        if (isBlackListed(mc.level.getBlockState(blockAim.neighbour)) && !mc.player.isShiftKeyDown()) {
            sendSneakInput(true);
            sneak = true;
        }

        rightClickBlock(blockAim.neighbour, blockAim.vec, blockAim.opposite, packetPlace.get());

        if (sneak) {
            sendSneakInput(false);
        }

        if (placeSwing.get()) {
            swing();
        }

        tick = 0;
    }

    private void openBlock() {
        sendSneakInput(false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(blockAim.vec, blockAim.opposite, blockAim.pos, false));
    }

    private void rightClickBlock(BlockPos pos, Vec3 vec, Direction direction, boolean packet) {
        BlockHitResult hit = new BlockHitResult(vec, direction, pos, false);
        if (packet) {
            mc.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, 0));
        } else {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        }
    }

    private void swing() {
        if (packetSwing.get()) {
            mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        } else {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private void sendSneakInput(boolean sneak) {
        ClientInput ci = mc.player.input;
        Input current = ci.keyPresses;
        mc.getConnection().send(new ServerboundPlayerInputPacket(
            new Input(current.forward(), current.backward(), current.left(), current.right(), current.jump(), sneak, current.sprint())
        ));
    }

    private boolean isBlackListed(BlockState state) {
        return state.is(Blocks.CHEST)
            || state.is(Blocks.TRAPPED_CHEST)
            || state.is(Blocks.ENDER_CHEST)
            || state.is(Blocks.CRAFTING_TABLE)
            || state.is(Blocks.FURNACE)
            || state.is(Blocks.BLAST_FURNACE)
            || state.is(Blocks.SMOKER)
            || state.is(Blocks.ANVIL)
            || state.is(Blocks.CHIPPED_ANVIL)
            || state.is(Blocks.DAMAGED_ANVIL)
            || state.is(Blocks.BARREL)
            || state.is(Blocks.ENCHANTING_TABLE)
            || state.is(Blocks.HOPPER)
            || state.is(Blocks.DISPENSER)
            || state.is(Blocks.DROPPER)
            || state.is(Blocks.LEVER)
            || state.is(Blocks.STONE_BUTTON)
            || state.is(Blocks.OAK_BUTTON)
            || state.is(Blocks.SPRUCE_BUTTON)
            || state.is(Blocks.BIRCH_BUTTON)
            || state.is(Blocks.JUNGLE_BUTTON)
            || state.is(Blocks.ACACIA_BUTTON)
            || state.is(Blocks.DARK_OAK_BUTTON)
            || state.is(Blocks.MANGROVE_BUTTON)
            || state.is(Blocks.CHERRY_BUTTON)
            || state.is(Blocks.PALE_OAK_BUTTON)
            || state.is(Blocks.BAMBOO_BUTTON)
            || state.getBlock() instanceof ShulkerBoxBlock;
    }

    private boolean canReplace(BlockPos pos) {
        return mc.level.getBlockState(pos).canBeReplaced();
    }

    private boolean canBeClicked(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(mc.level, pos).isEmpty();
    }

    private boolean isAir(BlockPos pos) {
        return mc.level.getBlockState(pos).isAir();
    }

    private Player getNearestPlayer(double maxRange) {
        Player nearest = null;
        double nearestDist = maxRange;
        for (Player player : mc.level.players()) {
            if (player != mc.player) {
                double dist = mc.player.distanceTo(player);
                if (dist < nearestDist) {
                    nearest = player;
                    nearestDist = dist;
                }
            }
        }
        return nearest;
    }

    private List<BlockPos> getSphere(Vec3 center, double radius, double height) {
        List<BlockPos> blocks = new ArrayList<>();
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);
        for (int x = (int) (cx - radius); x <= cx + radius; x++) {
            for (int z = (int) (cz - radius); z <= cz + radius; z++) {
                for (int y = (int) (cy - height); y <= cy + height; y++) {
                    double dx = x + 0.5 - center.x;
                    double dy = y + 0.5 - center.y;
                    double dz = z + 0.5 - center.z;
                    if (Math.sqrt(dx * dx + dy * dy + dz * dz) <= radius) {
                        blocks.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return blocks;
    }

    private static class ShulkerPos {
        final BlockPos pos;
        final Direction facing;
        final Vec3 vec;
        final BlockPos neighbour;
        final Direction opposite;

        ShulkerPos(BlockPos pos, Direction facing, BlockPos neighbour, Direction opposite, Vec3 vec3d) {
            this.pos = pos;
            this.facing = facing;
            this.neighbour = neighbour;
            this.opposite = opposite;
            this.vec = vec3d;
        }

        double getRange(Player player) {
            return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
    }
}