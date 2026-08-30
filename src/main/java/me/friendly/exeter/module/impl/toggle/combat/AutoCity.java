package me.friendly.exeter.module.impl.toggle.combat;

import java.util.Comparator;
import java.util.List;

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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * AutoCity. Breaks the block under/near an enemy's feet to drop them into a hole (the "city"
 * move — combined with crystals it's the standard 5b5t kill setup).
 *
 * Logic per tick:
 *  - Target: nearest enemy within Target Range with a breakable block directly under their feet
 *    (or under the block they'll step to, with Predict on).
 *  - Prefer unbreakable-adjacent: the block must be supported by a solid neighbour so breaking
 *    it actually drops the player (not just floating).
 *  - Break: vanilla destroy path (startDestroyBlock → continueDestroyBlock) — server-validated
 *    dig timing, identical to manual mining. Optional silent rotation.
 *  - Cooldown: after a successful city break, wait Delay Ticks before the next attempt.
 *
 * Safety: skips friends, skips blocks that would drop the target onto us (Min Dist To Self),
 * skips obsidian/bedrock unless Allow Hard Blocks is on (slow to break through).
 */
public class AutoCity extends ToggleableModule {
    private final NumberProperty<Double> targetRange = new NumberProperty<>(4.5, 1.0, 6.0, "Target Range", "range", "r");
    private final NumberProperty<Integer> delayTicks = new NumberProperty<>(2, 0, 20, "Delay Ticks", "delay", "d");
    private final Property<Boolean> silentAim = new Property<>(true, "Silent Aim", "silentaim", "sa");
    private final Property<Boolean> predict = new Property<>(true, "Predict (next step)", "predict", "p");
    private final Property<Boolean> allowHardBlocks = new Property<>(false, "Allow Obsidian", "hard", "h");
    private final NumberProperty<Double> minDistToSelf = new NumberProperty<>(2.5, 0.0, 6.0, "Min Dist To Self", "minself", "ms");

    private int cooldown = 0;
    private BlockPos lastTarget = null;

    public AutoCity() {
        super("AutoCity", new String[]{"autocity", "city", "autocitybreaker"}, ModuleType.COMBAT);
        offerProperties(targetRange, delayTicks, silentAim, predict, allowHardBlocks, minDistToSelf);

        this.listeners.add(new Listener<TickEvent>("auto_city_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer self = minecraft.player;
                if (self == null || minecraft.level == null || minecraft.gameMode == null) return;
                if (minecraft.gui.screen() != null || self.isUsingItem() || self.isDeadOrDying()) return;

                if (cooldown > 0) {
                    cooldown--;
                    return;
                }

                var enemy = nearestEnemy(self);
                if (enemy == null) return;

                BlockPos under = enemy.blockPosition().below();
                if (predict.getValue()) {
                    // If the enemy is moving, target the block under their next step.
                    var vel = enemy.getDeltaMovement();
                    if (Math.abs(vel.x) > 0.05 || Math.abs(vel.z) > 0.05) {
                        BlockPos next = BlockPos.containing(
                                enemy.getX() + vel.x * 4, enemy.getY() - 0.5, enemy.getZ() + vel.z * 4);
                        if (!minecraft.level.getBlockState(next).isAir()) under = next;
                    }
                }

                if (!isCityable(under)) return;
                if (self.position().distanceTo(Vec3.atCenterOf(under)) < minDistToSelf.getValue()) return;
                if (!PlayerUtil.inRange(under, targetRange.getValue())) return;

                // Break via vanilla dig path (server-validated timing).
                final BlockPos cityTarget = under;
                final Direction face = Direction.UP; // dig from above
                if (silentAim.getValue()) {
                    double yaw = PlayerUtil.getYaw(cityTarget);
                    double pitch = PlayerUtil.getPitch(cityTarget);
                    PlayerUtil.withRotation(yaw, pitch, () -> dig(cityTarget, face));
                } else {
                    dig(cityTarget, face);
                }

                if (!under.equals(lastTarget)) {
                    lastTarget = under;
                } else {
                    // Same block, dig progress continues; block broken when vanilla completes it.
                }
            }
        });
    }

    private void dig(BlockPos pos, Direction face) {
        if (minecraft.level.getBlockState(pos).isAir()) {
            // Broken — cooldown and reset.
            if (lastTarget != null && lastTarget.equals(pos)) {
                cooldown = Math.max(1, delayTicks.getValue());
                lastTarget = null;
            }
            return;
        }
        if (lastTarget == null || !lastTarget.equals(pos)) {
            minecraft.gameMode.startDestroyBlock(pos, face);
            lastTarget = pos;
        } else {
            minecraft.gameMode.continueDestroyBlock(pos, face);
        }
    }

    private boolean isCityable(BlockPos pos) {
        var state = minecraft.level.getBlockState(pos);
        if (state.isAir()) return false;
        Block block = state.getBlock();
        if (block == Blocks.BEDROCK || block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
            return allowHardBlocks.getValue();
        }
        // Must have a solid neighbour below/side so breaking actually drops the player.
        return !minecraft.level.getBlockState(pos.below()).isAir();
    }

    private net.minecraft.world.entity.player.Player nearestEnemy(LocalPlayer self) {
        return minecraft.level.players().stream()
                .filter(p -> p != self && p.isAlive())
                .filter(p -> !Exeter.getInstance().getFriendManager().isFriend(p.getName().getString()))
                .filter(p -> self.distanceTo(p) <= targetRange.getValue())
                .min(Comparator.comparingDouble(p -> self.distanceTo(p)))
                .orElse(null);
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        lastTarget = null;
        if (minecraft.gameMode != null) minecraft.gameMode.stopDestroyBlock();
    }
}
