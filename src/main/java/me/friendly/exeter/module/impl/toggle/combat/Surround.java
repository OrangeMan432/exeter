package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import me.friendly.exeter.properties.EnumProperty;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Surround. Auto-places obsidian/ender-chest-grade blocks around your feet to blunt crystal
 * damage — the core defensive module of the 5b5t crystal meta.
 *
 * Logic per tick:
 *  - Compute the 4 horizontal neighbours of your feet block (N/E/S/W). These are the surfaces
 *    crystals are normally placed on to hit you.
 *  - For each empty/replaceable neighbour, place a block from the hotbar (obsidian preferred,
 *    then any Blast-Resistant block, then any BlockItem if Allow Any Block is on).
 *  - Placement uses the vanilla useItemOn path (server-validated) with optional silent
 *    rotation; one placement per tick, throttled by Delay Ticks.
 *  - Center: when enabled and you're not standing on the exact block center, gently pull
 *    movement toward the center so you stay inside the surround (classic "center" behavior).
 *
 * Safety: never places into your own bounding box, respects AntiCrystal's place timeout,
 * stops while any screen is open or while using an item.
 */
public class Surround extends ToggleableModule {
    private enum BlockPreference { OBSIDIAN_ONLY, BLAST_RESISTANT, ANY_BLOCK }

    private final EnumProperty<BlockPreference> blockPref = new EnumProperty<>(BlockPreference.BLAST_RESISTANT, "Block Pref", "pref", "p");
    private final Property<Boolean> center = new Property<>(true, "Center", "center", "c");
    private final NumberProperty<Double> centerStrength = new NumberProperty<>(0.28, 0.05, 1.0, "Center Strength", "strength", "s");
    private final NumberProperty<Integer> delayTicks = new NumberProperty<>(0, 0, 10, "Delay Ticks", "delay", "d");
    private final Property<Boolean> silentAim = new Property<>(true, "Silent Aim", "silentaim", "sa");
    private final Property<Boolean> onlyWhenHurt = new Property<>(false, "Only When Hurt", "hurt", "h");
    private final Property<Boolean> supportEchest = new Property<>(true, "Allow Ender Chest", "echest", "ec");

    private int cooldown = 0;

    public Surround() {
        super("Surround", new String[]{"surround", "sur", "obsidianarmor"}, ModuleType.COMBAT);
        offerProperties(blockPref, center, centerStrength, delayTicks, silentAim, onlyWhenHurt, supportEchest);

        this.listeners.add(new Listener<TickEvent>("surround_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null || minecraft.gameMode == null) return;
                if (minecraft.gui.screen() != null) return;
                if (player.isUsingItem() || player.isDeadOrDying()) return;
                if (onlyWhenHurt.getValue() && player.hurtTime <= 0 && player.getHealth() >= player.getMaxHealth() - 0.5f) {
                    // Healthy and un-hurt: idling in surround is fine, but skip placing.
                    if (cooldown > 0) cooldown--;
                    return;
                }

                // Center pull: nudge movement toward the center of the feet block.
                if (center.getValue()) {
                    Vec3 pos = player.position();
                    double cx = player.blockPosition().getX() + 0.5;
                    double cz = player.blockPosition().getZ() + 0.5;
                    double dx = cx - pos.x;
                    double dz = cz - pos.z;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > 0.05) {
                        double k = centerStrength.getValue();
                        player.setDeltaMovement(
                                player.getDeltaMovement().x + dx * k,
                                player.getDeltaMovement().y,
                                player.getDeltaMovement().z + dz * k);
                    }
                }

                if (cooldown > 0) {
                    cooldown--;
                    return;
                }

                // Respect AntiCrystal's place timeout (don't place into a fresh blast).
                var antiCrystal = Exeter.getInstance().getModuleManager().getModuleByAlias("anticrystal");
                if (antiCrystal instanceof AntiCrystal ac && ac.isPlaceBlocked()) return;

                BlockPos feet = player.blockPosition();
                Direction[] sides = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

                for (Direction side : sides) {
                    BlockPos pos = feet.relative(side);
                    if (!needsBlock(pos)) continue;
                    if (!PlayerUtil.inRange(pos, 5.0)) continue;

                    int slot = findBlockSlot(player);
                    if (slot == -1) return; // no blocks left

                    int original = player.getInventory().getSelectedSlot();
                    boolean swapped = slot != original;
                    if (swapped) PlayerUtil.swapTo(slot);

                    Direction face = adjacentFace(pos);
                    if (face == null) {
                        if (swapped) PlayerUtil.swapBack();
                        continue;
                    }

                    if (silentAim.getValue()) {
                        double yaw = PlayerUtil.getYaw(pos);
                        double pitch = PlayerUtil.getPitch(pos);
                        PlayerUtil.withRotation(yaw, pitch, () -> {
                            PlayerUtil.useItemOn(pos, face);
                            PlayerUtil.swingHand();
                        });
                    } else {
                        PlayerUtil.useItemOn(pos, face);
                        PlayerUtil.swingHand();
                    }

                    if (swapped) PlayerUtil.swapBack();
                    cooldown = Math.max(1, delayTicks.getValue());
                    return; // one placement per tick
                }
            }
        });
    }

    private boolean needsBlock(BlockPos pos) {
        var state = minecraft.level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private int findBlockSlot(LocalPlayer player) {
        BlockPreference pref = blockPref.getValue();
        // Pass 1: preferred block type; Pass 2 (fallback): acceptable types.
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i <= 8; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem bi)) continue;
                var block = bi.getBlock();

                boolean obsidian = block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN;
                boolean echest = block == Blocks.ENDER_CHEST;
                boolean blastResistant = obsidian || echest
                        || block == Blocks.ANCIENT_DEBRIS || block == Blocks.NETHERITE_BLOCK
                        || block == Blocks.RESPAWN_ANCHOR || block == Blocks.ENCHANTING_TABLE;

                boolean ok;
                if (pref == BlockPreference.OBSIDIAN_ONLY) {
                    ok = obsidian;
                } else if (pref == BlockPreference.BLAST_RESISTANT) {
                    ok = blastResistant || (pass == 1 && echest && supportEchest.getValue());
                } else {
                    ok = true;
                }
                if (ok) return i;
            }
            if (pref == BlockPreference.ANY_BLOCK) break; // single pass is enough
        }
        return -1;
    }

    private Direction adjacentFace(BlockPos target) {
        for (Direction d : Direction.values()) {
            BlockPos adj = target.relative(d);
            var state = minecraft.level.getBlockState(adj);
            if (!state.isAir() && !state.canBeReplaced()
                    && !state.getCollisionShape(minecraft.level, adj).isEmpty()) {
                return d.getOpposite();
            }
        }
        return null;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        cooldown = 0;
    }
}
