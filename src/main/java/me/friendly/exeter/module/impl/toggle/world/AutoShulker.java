package me.friendly.exeter.module.impl.toggle.world;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoShulker extends ToggleableModule {

    private final Property<Boolean> once = new Property<Boolean>(false, "Once");
    private final NumberProperty<Integer> emptySlots = new NumberProperty<Integer>(6, 1, 36, "Empty Slots");
    private final Property<Boolean> disableAfterDeath = new Property<Boolean>(true, "Disable After Death");
    private final NumberProperty<Double> range = new NumberProperty<Double>(5.0, 0.0, 10.0, "Range");
    private final NumberProperty<Double> yRange = new NumberProperty<Double>(5.0, 0.0, 10.0, "Y Range");
    private final NumberProperty<Double> targetRange = new NumberProperty<Double>(8.0, 0.0, 16.0, "Target Range");
    private final NumberProperty<Integer> tickDelay = new NumberProperty<Integer>(5, 0, 10, "Tick Delay");
    private final NumberProperty<Integer> openDelay = new NumberProperty<Integer>(5, 0, 10, "Open Delay");
    private final Property<Boolean> inventory = new Property<Boolean>(true, "Inventory");
    private final NumberProperty<Integer> slot = new NumberProperty<Integer>(1, 1, 9, "Slot");
    private final Property<Boolean> packetPlace = new Property<Boolean>(true, "Packet Place");
    private final Property<Boolean> placeSwing = new Property<Boolean>(true, "Place Swing");
    private final Property<Boolean> packetSwing = new Property<Boolean>(true, "Packet Swing");
    private final Property<Boolean> packetSwitch = new Property<Boolean>(true, "Packet Switch");

    private int delayTimeTicks;
    private BlockPos playerPos;
    private ShulkerPos blockAim;
    private final List<BlockPos> list = new ArrayList<>();
    private int slotIndex;
    private boolean swapped;
    private int tick;

    private final Listener<TickEvent> tickListener = new Listener<TickEvent>("auto_shulker_tick") {
        @Override
        public void call(TickEvent event) {
            onTick();
        }
    };

    public AutoShulker() {
        super("Auto Shulker", new String[]{"autoshulker", "auto-shulker"}, 0x00FF00, ModuleType.WORLD);
        this.offerProperties(once, emptySlots, disableAfterDeath, range, yRange, targetRange,
                tickDelay, openDelay, inventory, slot, packetPlace, placeSwing, packetSwing, packetSwitch);
        this.listeners.add(tickListener);
    }

    @Override
    protected void onEnable() {
        blockAim = null;
        slotIndex = -1;
        swapped = false;
        tick = 0;
        delayTimeTicks = 0;
        checkPos();
        super.onEnable();
    }

    @Override
    protected void onDisable() {
        list.clear();
        blockAim = null;
        super.onDisable();
    }

    private void onTick() {
        if (minecraft.player == null) return;

        if (!once.getValue() && disableAfterDeath.getValue() && minecraft.player.isDeadOrDying()) {
            toggle();
            return;
        }

        if (tick++ >= openDelay.getValue()) {
            if (blockAim != null && inRange(blockAim.pos) && !isAir(blockAim.pos) && !canReplace(blockAim.pos)) {
                openBlock();
            }
            tick = 0;
        }

        if (minecraft.gui.screen() instanceof ShulkerBoxScreen) {
            if (once.getValue()) {
                toggle();
            }
            blockAim = null;
        } else if (delayTimeTicks++ >= tickDelay.getValue()) {
            delayTimeTicks = 0;
            if ((slotIndex = getShulkerSlot()) != -1) {
                if (!once.getValue() && getEmptyCounts() < emptySlots.getValue()) {
                    checkPos();
                } else if (blockAim == null) {
                    initValues();
                }

                if (blockAim == null) {
                    if (once.getValue()) {
                        toggle();
                    }
                } else if (!inRange(blockAim.pos)) {
                    blockAim = null;
                } else {
                    if (slotIndex > 8 && !swapped) {
                        if (!inventory.getValue()) {
                            return;
                        }

                        minecraft.gameMode.handleContainerInput(
                                minecraft.player.containerMenu.containerId,
                                slotIndex,
                                slot.getValue() - 1,
                                ContainerInput.SWAP,
                                minecraft.player
                        );
                        swapped = true;
                        if (tickDelay.getValue() != 0) {
                            return;
                        }
                    }

                    if (!isAir(blockAim.pos) && !canReplace(blockAim.pos)) {
                        openBlock();
                    } else {
                        switchTo(slotIndex, this::placeBlock);
                        if (tickDelay.getValue() == 0) {
                            openBlock();
                        }
                    }
                }
            }
        }
    }

    private void switchTo(int targetSlot, Runnable runnable) {
        if (targetSlot >= 0 && targetSlot != minecraft.player.getInventory().getSelectedSlot()) {
            if (targetSlot < 9) {
                if (packetSwitch.getValue()) {
                    minecraft.getConnection().send(new ServerboundSetCarriedItemPacket(targetSlot));
                    runnable.run();
                    minecraft.getConnection().send(new ServerboundSetCarriedItemPacket(minecraft.player.getInventory().getSelectedSlot()));
                } else {
                    minecraft.player.getInventory().setSelectedSlot(targetSlot);
                    runnable.run();
                }
            }
        } else {
            runnable.run();
        }
    }

    private int getShulkerSlot() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = minecraft.player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock) {
                return i;
            }
        }
        return -1;
    }

    private int getEmptyCounts() {
        int count = 0;
        for (int i = 0; i <= 35; i++) {
            if (minecraft.player.getInventory().getItem(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void initValues() {
        List<BlockPos> blocks = getSphere(minecraft.player.getEyePosition(), range.getValue() + 1.0, yRange.getValue() + 1.0);
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

        var target = getNearestPlayer(12.0);
        if (target == null) {
            blockAim = posList.stream().min(Comparator.comparing(p -> p.getRange(minecraft.player))).orElse(null);
        } else {
            blockAim = posList.stream().max(Comparator.comparing(p -> getWeight(p, target))).orElse(null);
        }

        if (blockAim != null) {
            list.add(blockAim.pos);
        }
    }

    private double getWeight(ShulkerPos pos, net.minecraft.world.entity.player.Player target) {
        double r = pos.getRange(target);
        if (r >= targetRange.getValue()) {
            int y = 256 - pos.pos.getY();
            r += y * 100;
        }
        return r;
    }

    private boolean intersectsWithEntity(BlockPos pos) {
        return minecraft.level.getEntities((Entity) null, new AABB(pos)).stream().anyMatch(entity -> !(entity instanceof ItemEntity));
    }

    private Direction getFacing(BlockPos pos) {
        if (!intersectsWithEntity(pos)
                && (canReplace(pos) || minecraft.level.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock)
                && minecraft.level.getBlockState(pos.above()).isAir()) {
            for (Direction facing : Direction.values()) {
                BlockPos neighbour = pos.relative(facing);
                if (canBeClicked(neighbour) && minecraft.level.getBlockState(neighbour.below()).isAir()) {
                    return facing;
                }
            }
        }
        return null;
    }

    private boolean inRange(Vec3 vec) {
        double x = vec.x - minecraft.player.getX();
        double z = vec.z - minecraft.player.getZ();
        double y = vec.y - minecraft.player.getEyePosition().y;
        double add = Math.sqrt(y * y) / 2.0;
        return x * x + z * z <= (range.getValue() - add) * (range.getValue() - add) && y * y <= yRange.getValue() * yRange.getValue();
    }

    private boolean inRange(BlockPos pos) {
        double x = pos.getX() + 0.5 - minecraft.player.getX();
        double z = pos.getZ() + 0.5 - minecraft.player.getZ();
        double y = pos.getY() + 0.5 - minecraft.player.getEyePosition().y;
        double add = Math.sqrt(y * y) / 2.0;
        return x * x + z * z <= (range.getValue() - add) * (range.getValue() - add) && y * y <= yRange.getValue() * yRange.getValue();
    }

    private void checkPos() {
        if (minecraft.player.blockPosition() != null && !minecraft.player.blockPosition().equals(playerPos)) {
            list.clear();
            playerPos = minecraft.player.blockPosition();
        }
    }

    private void placeBlock() {
        BlockHitResult hit = new BlockHitResult(blockAim.vec, blockAim.opposite, blockAim.neighbour, false);
        if (packetPlace.getValue()) {
            minecraft.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, 0));
        } else {
            minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit);
        }

        if (placeSwing.getValue()) {
            if (packetSwing.getValue()) {
                minecraft.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            } else {
                minecraft.player.swing(InteractionHand.MAIN_HAND);
            }
        }

        tick = 0;
    }

    private void openBlock() {
        BlockHitResult hit = new BlockHitResult(blockAim.vec, blockAim.opposite, blockAim.pos, false);
        minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit);
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
        return minecraft.level.getBlockState(pos).canBeReplaced();
    }

    private boolean canBeClicked(BlockPos pos) {
        BlockState state = minecraft.level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(minecraft.level, pos).isEmpty();
    }

    private boolean isAir(BlockPos pos) {
        return minecraft.level.getBlockState(pos).isAir();
    }

    private net.minecraft.world.entity.player.Player getNearestPlayer(double maxRange) {
        net.minecraft.world.entity.player.Player nearest = null;
        double nearestDist = maxRange;
        for (var player : minecraft.level.players()) {
            if (player != minecraft.player) {
                double dist = minecraft.player.distanceTo(player);
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

        double getRange(net.minecraft.world.entity.player.Player player) {
            return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
    }
}
