package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.client.player.LocalPlayer;

/**
 * AntiVoid. Recovers you when you fall through the world (5b5t void fights, chunk glitches,
 * crystal knockback through the nether roof).
 *
 * Modes:
 *  - Upward : applies upward velocity when below the configured Y (bounce back up).
 *  - Teleport: teleports to the last safe on-ground position (may rubber-band; use when
 *              already taking void damage).
 *
 * Trigger: player Y below the threshold AND no solid block found below within the scan depth
 * (so walking into a 1-block hole doesn't trigger it).
 */
public class AntiVoid extends ToggleableModule {
    private enum Mode { UPWARD, TELEPORT }

    private final EnumProperty<Mode> mode = new EnumProperty<>(Mode.UPWARD, "Mode", "m");
    private final NumberProperty<Double> triggerY = new NumberProperty<>(-1.0, -128.0, 320.0, "Trigger Y", "y", "trigger");
    private final NumberProperty<Double> upwardSpeed = new NumberProperty<>(0.42, 0.1, 2.0, "Upward Speed", "speed");
    private final NumberProperty<Integer> scanDepth = new NumberProperty<>(24, 4, 128, "Scan Depth", "scan");

    private double lastSafeX, lastSafeY, lastSafeZ;
    private boolean hasSafe;

    public AntiVoid() {
        super("AntiVoid", new String[]{"antivoid", "voidcheck"}, ModuleType.MOVEMENT);
        offerProperties(mode, triggerY, upwardSpeed, scanDepth);

        this.listeners.add(new Listener<TickEvent>("anti_void_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;

                if (player.onGround() && !minecraft.level.getBlockState(player.blockPosition()).isAir()) {
                    lastSafeX = player.getX();
                    lastSafeY = player.getY();
                    lastSafeZ = player.getZ();
                    hasSafe = true;
                }

                if (player.getY() > triggerY.getValue()) return;
                if (hasGroundBelow(player)) return;

                if (mode.getValue() == Mode.UPWARD) {
                    player.setDeltaMovement(player.getDeltaMovement().x, upwardSpeed.getValue(), player.getDeltaMovement().z);
                } else if (hasSafe) {
                    player.setPos(lastSafeX, lastSafeY, lastSafeZ);
                    player.setDeltaMovement(0, 0, 0);
                }
            }
        });
    }

    private boolean hasGroundBelow(LocalPlayer player) {
        int baseY = player.getBlockY();
        for (int i = 0; i < scanDepth.getValue(); i++) {
            var pos = player.blockPosition().below(i);
            var state = minecraft.level.getBlockState(pos);
            if (!state.getCollisionShape(minecraft.level, pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        hasSafe = false;
    }
}
