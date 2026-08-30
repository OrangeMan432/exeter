package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/**
 * AntiAFK. Prevents the 5b5t AFK kicker from flagging you while you farm/grind.
 *
 * Actions (subtle, server-visible, no packet spoofing):
 *  - Swing : swing the main hand on a randomized interval (5-15s).
 *  - Jump  : hop occasionally (2-8s) when on ground.
 *  - Rotate: nudge the view yaw a few degrees on a randomized interval.
 *
 * Intervals are randomized so the pattern isn't a fixed-timer signature.
 */
public class AntiAFK extends ToggleableModule {
    private final Property<Boolean> swing = new Property<>(true, "Swing", "swing");
    private final Property<Boolean> jump = new Property<>(true, "Jump", "jump");
    private final Property<Boolean> rotate = new Property<>(true, "Rotate", "rotate");
    private final Property<Boolean> onlyWhenIdle = new Property<>(true, "Only When Idle", "idle", "i");
    private final Property<Boolean> skipWhenScreenOpen = new Property<>(true, "Skip When Screen Open", "skipscreen");

    private int swingTimer = rand(100, 300);
    private int jumpTimer = rand(40, 160);
    private int rotateTimer = rand(60, 200);

    public AntiAFK() {
        super("AntiAFK", new String[]{"antiafk", "afk"}, ModuleType.MISCELLANEOUS);
        offerProperties(swing, jump, rotate, onlyWhenIdle, skipWhenScreenOpen);

        this.listeners.add(new Listener<TickEvent>("anti_afk_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;
                if (skipWhenScreenOpen.getValue() && minecraft.gui.screen() != null) return;
                // Only When Idle: skip if the player is already moving/using an item —
                // they're clearly not AFK, and extra swings would look robotic.
                if (onlyWhenIdle.getValue()) {
                    if (Math.abs(player.getDeltaMovement().x) > 0.05
                            || Math.abs(player.getDeltaMovement().z) > 0.05
                            || player.isUsingItem()) return;
                }

                if (swing.getValue() && --swingTimer <= 0) {
                    player.swing(InteractionHand.MAIN_HAND);
                    swingTimer = rand(100, 300);
                }

                if (jump.getValue() && --jumpTimer <= 0) {
                    if (player.onGround()) {
                        player.jumpFromGround();
                    }
                    jumpTimer = rand(40, 160);
                }

                if (rotate.getValue() && --rotateTimer <= 0) {
                    player.setYRot(player.getYRot() + (float) (Math.random() * 6.0 - 3.0));
                    rotateTimer = rand(60, 200);
                }
            }
        });
    }

    private static int rand(int min, int max) {
        return min + (int) (Math.random() * (max - min));
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        swingTimer = rand(100, 300);
        jumpTimer = rand(40, 160);
        rotateTimer = rand(60, 200);
    }
}
