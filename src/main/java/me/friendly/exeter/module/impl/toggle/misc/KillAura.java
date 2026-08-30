package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
 */
public class KillAura extends ToggleableModule {
    private final Property<Double> range = new Property<>(4.5, "Range", "range");
    private final Property<Integer> delay = new Property<>(4, "Delay", "delay");
    private int cooldown = 0;

    public KillAura() {
        super("KillAura", new String[]{"killaura", "ka"}, ModuleType.COMBAT);
        offerProperties(range, delay);

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
                    if (e instanceof LivingEntity living && e != minecraft.player
                            && living.isAlive() && !e.isSpectator()) {
                        candidates.add(living);
                    }
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

                minecraft.gameMode.attack(minecraft.player, target);
                minecraft.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                cooldown = Math.max(1, delay.getValue());
            }
        });
    }
}
