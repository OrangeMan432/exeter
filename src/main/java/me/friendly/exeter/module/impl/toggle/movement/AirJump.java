package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;

/**
 * AirJump. Allows jumping while airborne (mid-air hops).
 *
 * Sends the vanilla jump impulse whenever jump is held and the upward velocity has decayed,
 * giving a controlled double-jump chain rather than a per-tick rocket. Uses
 * {@code jumpFromGround()} so the server sees a legitimate jump motion each time; vertical
 * speed is reset first so consecutive hops stay consistent.
 *
 * Legit mode: only jumps once per air-time (a single mid-air hop) instead of a continuous chain.
 */
public class AirJump extends ToggleableModule {
    private final Property<Boolean> legit = new Property<>(true, "Legit", "legit", "l");
    private boolean jumpedThisAir = false;

    public AirJump() {
        super("AirJump", new String[]{"airjump", "doublejump"}, ModuleType.MOVEMENT);
        offerProperties(legit);

        this.listeners.add(new Listener<TickEvent>("air_jump_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;
                if (minecraft.gui.screen() != null) return;

                if (player.onGround()) {
                    jumpedThisAir = false;
                    return;
                }
                if (legit.getValue() && jumpedThisAir) return;
                if (player.input == null || player.input.keyPresses == null) return;
                if (!player.input.keyPresses.jump()) return;
                if (player.getDeltaMovement().y > 0.1) return; // still rising from last jump

                player.setDeltaMovement(player.getDeltaMovement().x, 0.42, player.getDeltaMovement().z);
                player.jumpFromGround();
                jumpedThisAir = true;
            }
        });
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        jumpedThisAir = false;
    }
}
