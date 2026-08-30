package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;

/**
 * AutoJump. Jumps automatically whenever you're moving and standing on ground — no hold-to-hop.
 *
 * Reuses the vanilla jump path ({@code jumpFromGround()}), so jump boost, exhaustion and
 * server-side validation behave exactly like a manual jump. Skipped while any screen is open
 * (chat wouldn't be sprint-jumpable otherwise) and while using items (bows/pearls/food).
 */
public class AutoJump extends ToggleableModule {
    private final Property<Boolean> onlyWhenMoving = new Property<>(true, "Only When Moving", "moving");
    private final Property<Boolean> skipUsingItem = new Property<>(true, "Skip While Using Item", "skipusing");

    public AutoJump() {
        super("AutoJump", new String[]{"autojump", "ajump"}, ModuleType.MOVEMENT);
        offerProperties(onlyWhenMoving, skipUsingItem);

        this.listeners.add(new Listener<TickEvent>("auto_jump_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;
                if (minecraft.gui.screen() != null) return;
                if (skipUsingItem.getValue() && player.isUsingItem()) return;
                if (!player.onGround()) return;
                if (player.input == null || player.input.keyPresses == null) return;

                Input keys = player.input.keyPresses;
                boolean moving = keys.forward() || keys.backward() || keys.left() || keys.right();
                if (onlyWhenMoving.getValue() ? moving : true) {
                    player.jumpFromGround();
                }
            }
        });
    }
}
