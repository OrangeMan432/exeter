package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;

/**
 * FastFall. Accelerates downward movement while airborne (dive back to the ground fast —
 * for resetting crystal timings and escaping 5b5t void fights).
 *
 * Applies extra downward velocity while the player holds sneak mid-air (configurable trigger),
 * capped so the server's per-tick movement tolerance isn't blown (vanilla accepts up to
 * ~0.98/tick vertical without rubber-banding on most anticheats).
 */
public class FastFall extends ToggleableModule {
    private final Property<Boolean> requireSneak = new Property<>(true, "Require Sneak", "sneak", "sneakkey");
    private final Property<Boolean> requireForward = new Property<>(true, "Require Forward", "forward", "fwd");
    private final Property<Double> speed = new Property<>(0.62, "Speed", "speed", "s");

    public FastFall() {
        super("FastFall", new String[]{"fastfall", "dive"}, ModuleType.MOVEMENT);
        offerProperties(requireSneak, requireForward, speed);

        this.listeners.add(new Listener<TickEvent>("fast_fall_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;
                if (player.onGround()) return;
                if (player.isInWater() || player.isInLava()) return;
                if (player.isFallFlying()) return; // elytra handled separately
                if (player.input == null || player.input.keyPresses == null) return;

                Input keys = player.input.keyPresses;
                if (requireSneak.getValue() && !keys.shift()) return;
                if (requireForward.getValue() && !keys.forward()) return;
                if (keys.jump()) return; // don't fight an intentional upward jump

                double v = player.getDeltaMovement().y;
                double target = -Math.abs(speed.getValue());
                if (v > target) {
                    player.setDeltaMovement(player.getDeltaMovement().x, target, player.getDeltaMovement().z);
                }
            }
        });
    }
}
