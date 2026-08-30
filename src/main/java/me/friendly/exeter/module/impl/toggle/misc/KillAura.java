package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * KillAura. Attacks the nearest living entity in range every few ticks.
 *
 * Uses the server-validated attack path ({@code MultiPlayerGameMode.attack}) which sends the
 * interaction packet AND the swing, and the server validates reach/LOS exactly as a manual
 * click. ({@code Player.attack(Entity)} alone is client-side only: it plays the swing
 * animation locally and never tells the server, so hits never registered.)
 *
 * Target selection uses squared distances (no wasted sqrt) and the server's own
 * {@code isWithinEntityInteractionRange} as the final gate, so a target outside the
 * server's authoritative reach is never attempted.
 *
 * Customizability:
 *  - Targets   : Players only (5b5t PvP) / Mobs only / Both.
 *  - Skip Friends : never target friends (FriendManager).
 *  - Range / Delay : reach and attack interval in ticks.
 *  - Require LOS : skip targets behind walls (off = hit through blocks, riskier).
 */
public class KillAura extends ToggleableModule {
    private enum Targets { PLAYERS, MOBS, BOTH }

    private final EnumProperty<Targets> targets = new EnumProperty<>(Targets.PLAYERS, "Targets", "targets", "t");
    private final Property<Boolean> skipFriends = new Property<>(true, "Skip Friends", "skipfriends", "sf");
    private final NumberProperty<Double> range = new NumberProperty<>(4.5, 1.0, 6.0, "Range", "range");
    private final NumberProperty<Integer> delay = new NumberProperty<>(4, 1, 40, "Delay", "delay");
    private final Property<Boolean> requireLos = new Property<>(true, "Require LOS", "los");
    private int cooldown = 0;

    public KillAura() {
        super("KillAura", new String[]{"killaura", "ka"}, ModuleType.COMBAT);
        offerProperties(targets, skipFriends, range, delay, requireLos);

        this.listeners.add(new Listener<TickEvent>("kill_aura_tick") {
            @Override
            public void call(TickEvent event) {
                if (minecraft.player == null || minecraft.level == null) return;
                if (minecraft.gameMode == null) return;
                if (cooldown > 0) {
                    cooldown--;
                    return;
                }

                double r = range.getValue();
                double rSqr = r * r;
                AABB box = AABB.ofSize(minecraft.player.position(), r * 2.0, r * 2.0, r * 2.0);

                List<LivingEntity> candidates = new ArrayList<>();
                for (Entity e : minecraft.level.getEntities(minecraft.player, box, ent -> true)) {
                    if (!(e instanceof LivingEntity living) || e == minecraft.player
                            || !living.isAlive() || e.isSpectator()) continue;
                    boolean isPlayer = e instanceof Player;
                    if (targets.getValue() == Targets.PLAYERS && !isPlayer) continue;
                    if (targets.getValue() == Targets.MOBS && isPlayer) continue;
                    if (skipFriends.getValue() && isPlayer
                            && Exeter.getInstance().getFriendManager().isFriend(((Player) e).getName().getString())) continue;
                    candidates.add(living);
                }
                if (candidates.isEmpty()) return;

                LivingEntity target = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity e : candidates) {
                    double dSqr = minecraft.player.distanceToSqr(e);
                    if (dSqr <= rSqr && dSqr < best) {
                        best = dSqr;
                        target = e;
                    }
                }
                if (target == null) return;
                // Server-authoritative reach gate: skip targets the server would reject.
                if (!minecraft.player.isWithinEntityInteractionRange(target, r)) return;
                if (requireLos.getValue() && !minecraft.player.hasLineOfSight(target)) return;

                minecraft.gameMode.attack(minecraft.player, target);
                minecraft.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                cooldown = Math.max(1, delay.getValue());
            }
        });
    }
}
