package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;

/**
 * Sprint. Keeps you sprinting whenever you're moving forward — no double-tap W, no holding
 * the sprint key.
 *
 * How it works on 26.2: the client's movement comes from {@code LocalPlayer.input.keyPresses}
 * (an immutable {@link Input} record: forward/backward/left/right/jump/shift/sprint). The
 * module rewrites that record every tick with {@code sprint=true} before the player applies
 * it, so the movement code sprints whenever there is forward impulse. The local view is the
 * source of truth here — this only removes the key-holding requirement; the server still
 * validates exhaustion and collision exactly as if the key were held.
 *
 * Modes:
 *  - Omni : sprint in every direction (forward/back/strafe).
 *  - Forward : sprint only when moving forward (vanilla-like).
 *
 * Safety: sprinting is suppressed while a screen is open (typing in chat would otherwise
 * sprint you around) and while the player is null (menus).
 */
public class Sprint extends ToggleableModule {
    private enum SprintMode {
        OMNI, FORWARD
    }

    private final EnumProperty<SprintMode> mode = new EnumProperty<>(SprintMode.OMNI, "Mode", "mode");
    private final Property<Boolean> onlyWhenMoving = new Property<>(true, "Only When Moving", "onlymoving");

    public Sprint() {
        super("Sprint", new String[]{"sprint", "autosprint"}, ModuleType.MOVEMENT);
        offerProperties(mode, onlyWhenMoving);

        this.listeners.add(new Listener<TickEvent>("sprint_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null) return;
                if (minecraft.gui.screen() != null) return;
                if (player.input == null || player.input.keyPresses == null) return;

                Input keys = player.input.keyPresses;
                if (!keys.forward() && !keys.backward() && !keys.left() && !keys.right()) {
                    return; // standing still: nothing to sprint
                }
                if (onlyWhenMoving.getValue() && !keys.forward() && !keys.backward()) {
                    return; // pure strafe, and Only When Moving is on
                }

                boolean omni = mode.getValue() == SprintMode.OMNI;
                if (!omni && !keys.forward()) return; // Forward mode: forward impulse only

                if (keys.sprint()) return; // already sprinting (key held) — no rewrite

                // Input is a record: rebuild it with sprint=true, same everything else.
                player.input.keyPresses = new Input(
                        keys.forward(), keys.backward(), keys.left(), keys.right(),
                        keys.jump(), keys.shift(), true);
            }
        });
    }
}
