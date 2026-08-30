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
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * AutoCrystal. Places and detonates end crystals on targets (the 5b5t crystal meta, automated).
 *
 * Loop per tick:
 *  1. Break: nearest crystal within Break Range that damages an enemy more than us → attack
 *     packet (server-validated, same as a manual click).
 *  2. Place: find a spot adjacent to a solid block, within Place Range, where a crystal would
 *     damage the nearest enemy above the Min Damage threshold and damage us below Max Self
 *     Damage → swing + use-item-on (crystal item required in hand/offhand).
 *
 * Damage model: the vanilla explosion exposure approximation
 * ({@code ExplosionDamageCalculator}) is too heavy to replicate exactly client-side, so this
 * uses a distance-based heuristic: damage ≈ 60 × (1 - dist/12) clamped — tuned for 5b5t
 * crystal fights (crystals do up to ~57 damage point-blank unarmored). Place requires line of
 * sight to the target position (no placing through walls into void pockets).
 */
public class AutoCrystal extends ToggleableModule {
    private final NumberProperty<Double> placeRange = new NumberProperty<>(4.5, 1.0, 6.0, "Place Range", "placerange", "pr");
    private final NumberProperty<Double> breakRange = new NumberProperty<>(4.5, 1.0, 6.0, "Break Range", "breakrange", "br");
    private final NumberProperty<Double> minDamage = new NumberProperty<>(4.0, 0.0, 20.0, "Min Damage", "mindmg");
    private final NumberProperty<Double> maxSelfDamage = new NumberProperty<>(8.0, 0.0, 20.0, "Max Self Damage", "maxself");
    private final Property<Boolean> place = new Property<>(true, "Place", "place", "p");
    private final Property<Boolean> attack = new Property<>(true, "Attack", "attack", "a");
    private final Property<Boolean> noSuicide = new Property<>(true, "No Suicide", "nosuicide", "ns");
    private final NumberProperty<Integer> placeDelay = new NumberProperty<>(2, 0, 20, "Place Delay", "placedelay");

    private int placeCooldown = 0;

    public AutoCrystal() {
        super("AutoCrystal", new String[]{"autocrystal", "crystalaura", "ca"}, ModuleType.COMBAT);
        offerProperties(place, attack, placeRange, breakRange, minDamage, maxSelfDamage, noSuicide, placeDelay);

        this.listeners.add(new Listener<TickEvent>("auto_crystal_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer self = minecraft.player;
                if (self == null || minecraft.level == null || minecraft.gameMode == null) return;
                if (self.isDeadOrDying()) return;

                if (attack.getValue()) {
                    breakCrystals(self);
                }
                if (place.getValue()) {
                    if (placeCooldown > 0) {
                        placeCooldown--;
                    } else {
                        placeCrystal(self);
                    }
                }
            }
        });
    }

    private void breakCrystals(LocalPlayer self) {
        EndCrystal best = null;
        double bestScore = -Double.MAX_VALUE;

        for (Entity e : minecraft.level.entitiesForRendering()) {
            if (!(e instanceof EndCrystal crystal) || !crystal.isAlive()) continue;
            if (self.distanceTo(crystal) > breakRange.getValue()) continue;

            double dmgToEnemy = bestEnemyDamage(self, crystal.position());
            double dmgToSelf = approxDamage(self.position(), crystal.position());
            if (noSuicide.getValue() && dmgToSelf >= self.getHealth()) continue;
            if (dmgToSelf > maxSelfDamage.getValue()) continue;

            double score = dmgToEnemy - dmgToSelf;
            if (score > bestScore) {
                bestScore = score;
                best = crystal;
            }
        }

        if (best != null && bestScore >= minDamage.getValue()) {
            minecraft.gameMode.attack(self, best);
            self.swing(InteractionHand.MAIN_HAND);
        }
    }

    private void placeCrystal(LocalPlayer self) {
        if (findCrystalItem(self) == -1) return;

        Player target = nearestEnemy(self);
        if (target == null) return;

        BlockPos bestPos = null;
        double bestScore = minDamage.getValue(); // must at least exceed min damage

        BlockPos feet = self.blockPosition();
        int r = (int) Math.ceil(placeRange.getValue());
        for (BlockPos pos : BlockPos.betweenClosed(
                feet.offset(-r, -2, -r), feet.offset(r, 2, r))) {

            if (!isPlaceable(pos)) continue;
            if (!PlayerUtil.inRange(pos, placeRange.getValue())) continue;

            Vec3 center = Vec3.atCenterOf(pos);
            double dmgEnemy = approxDamage(target.position(), center);
            double dmgSelf = approxDamage(self.position(), center);
            if (dmgEnemy < bestScore) continue;
            if (noSuicide.getValue() && dmgSelf >= self.getHealth()) continue;
            if (dmgSelf > maxSelfDamage.getValue()) continue;
            if (!self.hasLineOfSight(target) && self.distanceTo(target) < 3.0) continue;

            bestScore = dmgEnemy;
            bestPos = pos.immutable();
        }

        if (bestPos == null) return;

        final BlockPos placePos = bestPos;
        Direction face = adjacentFace(placePos);
        if (face == null) return;

        int slot = findCrystalItem(self);
        int original = self.getInventory().getSelectedSlot();
        boolean swapped = slot != original;
        if (swapped) PlayerUtil.swapTo(slot);

        double yaw = PlayerUtil.getYaw(bestPos);
        double pitch = PlayerUtil.getPitch(bestPos);
        PlayerUtil.withRotation(yaw, pitch, () -> {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(placePos), face, placePos, false);
            minecraft.gameMode.useItemOn(self, InteractionHand.MAIN_HAND, hit);
            self.swing(InteractionHand.MAIN_HAND);
        });

        if (swapped) PlayerUtil.swapBack();
        placeCooldown = Math.max(1, placeDelay.getValue());
    }

    private boolean isPlaceable(BlockPos pos) {
        var state = minecraft.level.getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced()) return false;
        // Needs a solid neighbor to place against (crystals place on top of obsidian etc).
        for (Direction d : Direction.values()) {
            var adj = minecraft.level.getBlockState(pos.relative(d));
            if (adj.getBlock() == Blocks.OBSIDIAN
                    || adj.getBlock() == Blocks.BEDROCK
                    || adj.isRedstoneConductor(minecraft.level, pos.relative(d))) {
                return true;
            }
        }
        return false;
    }

    private Direction adjacentFace(BlockPos target) {
        for (Direction d : Direction.values()) {
            BlockPos adj = target.relative(d);
            var state = minecraft.level.getBlockState(adj);
            if (!state.isAir() && !state.getCollisionShape(minecraft.level, adj).isEmpty()) {
                return d.getOpposite();
            }
        }
        return null;
    }

    private int findCrystalItem(LocalPlayer player) {
        var inv = player.getInventory();
        int off = -1;
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).getItem() == Items.END_CRYSTAL) return i;
        }
        if (player.getOffhandItem().getItem() == Items.END_CRYSTAL) off = 45;
        return off != -1 ? off : -1;
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

    private double bestEnemyDamage(LocalPlayer self, Vec3 crystalPos) {
        double best = 0;
        for (Player p : minecraft.level.players()) {
            if (p == self || !p.isAlive()) continue;
            if (Exeter.getInstance().getFriendManager().isFriend(p.getName().getString())) continue;
            best = Math.max(best, approxDamage(p.position(), crystalPos));
        }
        return best;
    }

    /**
     * Distance-based crystal damage approximation: ~60 max damage at 0 distance, linear falloff
     * over 12 blocks (crystal explosion radius). Armor/toughness ignored — the ranking is what
     * matters, not exact numbers.
     */
    private static double approxDamage(Vec3 targetPos, Vec3 crystalPos) {
        double dist = targetPos.distanceTo(crystalPos);
        if (dist >= 12.0) return 0;
        return Math.max(0, 60.0 * (1.0 - dist / 12.0));
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        placeCooldown = 0;
    }
}
