package me.friendly.exeter.module.impl.toggle.combat;

import java.util.Comparator;

import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * HoleFiller. Fills nearby 1x1 holes with obsidian so enemies can't use them as crystal-safe
 * cover — the offensive complement to HoleSnap/Surround (a filled hole is a dead hole).
 *
 * Logic per tick:
 *  - Scan radius for 1x1 holes: air column at feet+head, solid floor, 4 solid walls.
 *  - Priority: holes nearest to an ENEMY first (denying them cover), then holes near us
 *    (removing retreat options is also denial). Never fills the hole WE are standing in,
 *    never fills one occupied by a friendly.
 *  - One placement per tick, throttled by Delay Ticks; vanilla useItemOn path with optional
 *    silent rotation.
 *
 * Safety: respects AntiCrystal's place timeout, pauses for screens/item use, skips friends'
 * holes, and skips holes occupied by any player (filling one under someone is a trap-grief
 * that 5b5t admins frown on and it wastes blocks).
 */
public class HoleFiller extends ToggleableModule {
    private final NumberProperty<Double> range = new NumberProperty<>(6.0, 2.0, 10.0, "Range", "range", "r");
    private final NumberProperty<Integer> delayTicks = new NumberProperty<>(2, 0, 10, "Delay Ticks", "delay", "d");
    private final Property<Boolean> silentAim = new Property<>(true, "Silent Aim", "silentaim", "sa");
    private final Property<Boolean> skipFriends = new Property<>(true, "Skip Friends' Holes", "friends", "f");
    private final Property<Boolean> onlyNearEnemy = new Property<>(false, "Only Near Enemy", "enemyonly", "eo");

    private int cooldown = 0;

    public HoleFiller() {
        super("HoleFiller", new String[]{"holefiller", "fillholes", "hf"}, ModuleType.COMBAT);
        offerProperties(range, delayTicks, silentAim, skipFriends, onlyNearEnemy);

        this.listeners.add(new Listener<TickEvent>("hole_filler_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer self = minecraft.player;
                if (self == null || minecraft.level == null || minecraft.gameMode == null) return;
                if (minecraft.gui.screen() != null || self.isUsingItem() || self.isDeadOrDying()) return;

                if (cooldown > 0) {
                    cooldown--;
                    return;
                }

                var antiCrystal = Exeter.getInstance().getModuleManager().getModuleByAlias("anticrystal");
                if (antiCrystal instanceof AntiCrystal ac && ac.isPlaceBlocked()) return;

                BlockPos hole = findBestHole(self);
                if (hole == null) return;
                if (placeFill(hole)) cooldown = Math.max(1, delayTicks.getValue());
            }
        });
    }

    private BlockPos findBestHole(LocalPlayer self) {
        Player nearestEnemy = nearestEnemy(self);
        if (onlyNearEnemy.getValue() && nearestEnemy == null) return null;

        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        int r = (int) Math.ceil(range.getValue());
        BlockPos feet = self.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(
                feet.offset(-r, -2, -r), feet.offset(r, 1, r))) {
            if (!isFillableHole(pos.immutable(), self)) continue;

            double distSelf = distXZ(self.position(), pos);
            // Score: enemy-proximity dominates (deny their cover first), self-distance breaks ties.
            double score = nearestEnemy != null
                    ? distXZ(nearestEnemy.position(), pos) * 2.0 + distSelf
                    : distSelf;
            if (score < bestScore) {
                bestScore = score;
                best = pos.immutable();
            }
        }
        return best;
    }

    /** A 1x1 hole: air at feet+head, solid floor, 4 solid walls, nobody inside. */
    private boolean isFillableHole(BlockPos feet, LocalPlayer self) {
        var level = minecraft.level;
        if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) return false;
        if (level.getBlockState(feet.below()).isAir()) return false;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(feet.relative(d)).isAir()) return false;
        }
        // Never fill a hole someone occupies (ours included).
        for (var p : level.players()) {
            if (!p.isAlive()) continue;
            if (p.blockPosition().equals(feet)) {
                if (p == self) return false;
                if (skipFriends.getValue()
                        && Exeter.getInstance().getFriendManager().isFriend(p.getName().getString())) return false;
                return false; // occupied by an enemy — filling it mid-fight wastes blocks
            }
        }
        return PlayerUtil.inRange(feet, range.getValue());
    }

    private boolean placeFill(BlockPos hole) {
        int slot = findBlockSlot();
        if (slot == -1) return false;

        // Fill at head level (the block that closes the hole for standing).
        BlockPos fillPos = hole.above();
        var state = minecraft.level.getBlockState(fillPos);
        if (!state.isAir() && !state.canBeReplaced()) return false;

        Direction face = adjacentFace(fillPos);
        if (face == null) {
            // Fall back to filling at feet level if the head-level cap has no support face.
            fillPos = hole;
            face = adjacentFace(fillPos);
            if (face == null) return false;
        }

        final BlockPos fillTarget = fillPos;
        final Direction fillFace = face;
        int original = minecraft.player.getInventory().getSelectedSlot();
        boolean swapped = slot != original;
        if (swapped) PlayerUtil.swapTo(slot);

        if (silentAim.getValue()) {
            double yaw = PlayerUtil.getYaw(fillTarget);
            double pitch = PlayerUtil.getPitch(fillTarget);
            PlayerUtil.withRotation(yaw, pitch, () -> {
                PlayerUtil.useItemOn(fillTarget, fillFace);
                PlayerUtil.swingHand();
            });
        } else {
            PlayerUtil.useItemOn(fillTarget, fillFace);
            PlayerUtil.swingHand();
        }

        if (swapped) PlayerUtil.swapBack();
        return true;
    }

    private int findBlockSlot() {
        var inv = minecraft.player.getInventory();
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem bi)) continue;
            var block = bi.getBlock();
            if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) return i;
        }
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return i;
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

    private Player nearestEnemy(LocalPlayer self) {
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player p : minecraft.level.players()) {
            if (p == self || !p.isAlive()) continue;
            if (Exeter.getInstance().getFriendManager().isFriend(p.getName().getString())) continue;
            double d = self.distanceTo(p);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private static double distXZ(net.minecraft.world.phys.Vec3 pos, BlockPos block) {
        double dx = block.getX() + 0.5 - pos.x;
        double dz = block.getZ() + 0.5 - pos.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        cooldown = 0;
    }
}
