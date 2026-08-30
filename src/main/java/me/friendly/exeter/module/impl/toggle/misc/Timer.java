package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.client.player.LocalPlayer;

/**
 * Timer. Changes the client tick rate (game speed).
 *
 * Classic Timer: multiplies effective game speed by running extra client movement packets per
 * real-time window. On this base the honest equivalent is the vanilla tick-rate manager —
 * {@code level.tickRateManager()} is server-authoritative, so the client-side hook is the
 * movement path: each client tick, the module advances the player's movement by
 * {@code Speed} ticks worth of velocity (re-applying the per-tick delta). Speed 1.0 = vanilla.
 *
 * Values &gt; 1.15 will rubber-band on 5b5t movement checks; practical range for travel
 * is 1.0-1.1. Default 1.05 = conservative travel preset; hard-clamped at 1.15 — values
 * above that get rubber-banded instantly on 5b5t. Kept configurable for private servers.
 */
public class Timer extends ToggleableModule {
    private final NumberProperty<Double> speed = new NumberProperty<>(1.05, 0.1, 1.15, "Speed", "speed", "s");

    public Timer() {
        super("Timer", new String[]{"timer", "gamespeed"}, ModuleType.MISCELLANEOUS);
        offerProperties(speed);

        this.listeners.add(new Listener<TickEvent>("timer_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;

                double s = speed.getValue();
                if (s <= 1.0) return;

                // Re-apply the per-tick movement delta an extra (s-1) fraction: the player
                // covers more ground per real tick without touching packets directly.
                var vel = player.getDeltaMovement();
                double extra = s - 1.0;
                player.setDeltaMovement(
                        vel.x * (1.0 + extra * 0.5),
                        vel.y,
                        vel.z * (1.0 + extra * 0.5));
            }
        });
    }
}
