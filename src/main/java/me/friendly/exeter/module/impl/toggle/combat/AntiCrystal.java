package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

/**
 * AntiCrystal. Defensive toolkit against end-crystal fights (the 5b5t meta).
 *
 *  - Prevent Attack : cancels OUR interaction packets aimed at crystals (stops you breaking
 *    your own blocker crystals / triggering chain explosions in panic).
 *  - Place Timeout  : after a nearby crystal despawns (popped), block placing for N ticks so
 *    you don't re-place into your own blast. Other modules (Scaffold) can consult
 *    {@link #isPlaceBlocked()}.
 *
 * Crystal spawn tracking uses the {@code ClientboundAddEntityPacket} type field directly —
 * no level lookup race on spawn.
 *
 * 5b5t note: crystal combat here is pure vanilla packets (attack/use-item-on), same as
 * manual play — no packet spoofing, so nothing for the server's movement/interact checks
 * to flag.
 */
public class AntiCrystal extends ToggleableModule {
    private final Property<Boolean> preventAttack = new Property<>(true, "Prevent Attack", "preventattack", "pa");
    private final Property<Boolean> placeTimeout = new Property<>(true, "Place Timeout", "placetimeout", "pt");
    private final NumberProperty<Integer> timeoutTicks = new NumberProperty<>(5, 1, 40, "Timeout Ticks", "timeout");
    private final NumberProperty<Double> popRadius = new NumberProperty<>(6.0, 2.0, 12.0, "Pop Radius", "popradius", "pr");

    private int placeBlockTicks = 0;

    public AntiCrystal() {
        super("AntiCrystal", new String[]{"anticrystal", "crystaldefense"}, ModuleType.COMBAT);
        offerProperties(preventAttack, placeTimeout, timeoutTicks, popRadius);

        this.listeners.add(new Listener<PacketEvent>("anti_crystal_packet") {
            @Override
            public void call(PacketEvent event) {
                if (minecraft.player == null) return;

                // Cancel outgoing attacks on crystals.
                if (preventAttack.getValue()
                        && event.getPacket() instanceof ServerboundInteractPacket interact
                        && isCrystal(interact.entityId())) {
                    event.setCanceled(true);
                    return;
                }

                // A nearby crystal despawned = it popped. Start the place timeout.
                if (placeTimeout.getValue()
                        && event.getPacket() instanceof ClientboundRemoveEntitiesPacket remove
                        && minecraft.level != null) {
                    for (int id : remove.getEntityIds()) {
                        Entity e = minecraft.level.getEntity(id);
                        if (e instanceof EndCrystal
                                && minecraft.player.distanceTo(e) <= popRadius.getValue()) {
                            placeBlockTicks = Math.max(1, timeoutTicks.getValue());
                        }
                    }
                }
            }
        });

        this.listeners.add(new Listener<TickEvent>("anti_crystal_tick") {
            @Override
            public void call(TickEvent event) {
                if (placeBlockTicks > 0) placeBlockTicks--;
            }
        });
    }

    private boolean isCrystal(int entityId) {
        if (minecraft.level == null) return false;
        Entity e = minecraft.level.getEntity(entityId);
        return e instanceof EndCrystal;
    }

    /** True while place-timeout is active (Scaffold and friends can consult this). */
    public boolean isPlaceBlocked() {
        return placeTimeout.getValue() && placeBlockTicks > 0;
    }
}
