package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

/**
 * ElytraFly. Enhances elytra flight with speed/control modes (nether-highway travel on 5b5t).
 *
 * Modes:
 *  - Boost : multiply forward velocity each tick while flying (rocket-free boost).
 *  - Control: creative-style flight — movement keys drive velocity directly, vertical handled
 *             by jump/sneak. No rockets burned.
 *  - Vanilla: no modification (parity mode).
 *
 * Safety: only acts while {@code isFallFlying()} is true with an elytra equipped (checked via
 * the chest slot item being {@code Items.ELYTRA} — on 26.2 elytra is a plain item with
 * equipment data, no dedicated ElytraItem class).
 *
 * 5b5t tuning (from the AnarchyExploitFixes config 5b5t runs):
 *  - old-chunk speed limit is ~1.81 → default cap 1.7 (safety margin)
 *  - burst windows allow ~3.12-5.0 at high TPS → optional Burst toggle
 *  - nether roof is hard-capped at 0.5 → auto-applied above y=127 in the nether
 *  - packet-elytra (glide-toggle) is detected (>25 opens/8s) → we never toggle gliding,
 *    all modes work on deltaMovement only.
 */
public class ElytraFly extends ToggleableModule {
    private enum Mode { BOOST, CONTROL, VANILLA }

    private final EnumProperty<Mode> mode = new EnumProperty<>(Mode.BOOST, "Mode", "m");
    private final NumberProperty<Double> boostFactor = new NumberProperty<>(1.35, 1.0, 3.0, "Boost Factor", "boost", "b");
    private final NumberProperty<Double> controlSpeed = new NumberProperty<>(1.8, 0.2, 5.0, "Control Speed", "speed", "s");
    private final Property<Boolean> verticalControl = new Property<>(true, "Vertical Control", "vertical", "v");
    private final Property<Boolean> stabilize = new Property<>(true, "Stabilize", "stabilize", "stab");
    private final NumberProperty<Double> speedCap = new NumberProperty<>(1.7, 0.4, 4.0, "Speed Cap", "cap");
    private final Property<Boolean> burst = new Property<>(false, "Burst (new chunks)", "burst", "b");
    private final Property<Boolean> netherRoofLimit = new Property<>(true, "Nether Roof Limit", "netherroof", "nr");

    public ElytraFly() {
        super("ElytraFly", new String[]{"elytrafly", "elytra"}, ModuleType.MOVEMENT);
        offerProperties(mode, boostFactor, controlSpeed, verticalControl, stabilize, speedCap, burst, netherRoofLimit);

        this.listeners.add(new Listener<TickEvent>("elytra_fly_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;
                if (!player.isFallFlying()) return;
                if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) return;

                var vel = player.getDeltaMovement();
                switch (mode.getValue()) {
                    case BOOST -> {
                        var look = player.getLookAngle();
                        double forwardSpeed = vel.x * look.x + vel.z * look.z;
                        if (forwardSpeed > 0.05) {
                            double gain = forwardSpeed * (boostFactor.getValue() - 1.0);
                            player.setDeltaMovement(vel.x + look.x * gain, vel.y, vel.z + look.z * gain);
                        }
                    }
                    case CONTROL -> {
                        double speed = controlSpeed.getValue();
                        double x = 0, z = 0, y = vel.y;
                        if (player.input != null && player.input.keyPresses != null) {
                            var keys = player.input.keyPresses;
                            float yaw = (float) Math.toRadians(player.getYRot());
                            if (keys.forward()) { x -= Math.sin(yaw) * speed; z += Math.cos(yaw) * speed; }
                            if (keys.backward()) { x += Math.sin(yaw) * speed * 0.8; z -= Math.cos(yaw) * speed * 0.8; }
                            if (keys.left()) { x += Math.cos(yaw) * speed * 0.8; z += Math.sin(yaw) * speed * 0.8; }
                            if (keys.right()) { x -= Math.cos(yaw) * speed * 0.8; z -= Math.sin(yaw) * speed * 0.8; }
                            if (verticalControl.getValue()) {
                                if (keys.jump()) y = speed * 0.6;
                                else if (keys.shift()) y = -speed * 0.6;
                                else y = 0;
                            }
                        }
                        player.setDeltaMovement(x, y, z);
                        player.fallDistance = 0;
                    }
                    case VANILLA -> { /* no-op */ }
                }

                if (stabilize.getValue() && mode.getValue() != Mode.VANILLA) {
                    var after = player.getDeltaMovement();
                    double horizontal = Math.sqrt(after.x * after.x + after.z * after.z);
                    double cap = effectiveCap(player);
                    if (horizontal > cap) {
                        double scale = cap / horizontal;
                        player.setDeltaMovement(after.x * scale, after.y, after.z * scale);
                    }
                }
            }
        });
    }

    /**
     * Cap effective this tick. Nether roof (y >= 127 in the nether) is hard-limited
     * to 0.5 like AEF does; Burst raises the cap to the new-chunk burst window (3.1)
     * which only stays safe in fresh chunks at good TPS.
     */
    private double effectiveCap(LocalPlayer player) {
        if (netherRoofLimit.getValue()
                && player.level().dimension() == net.minecraft.world.level.Level.NETHER
                && player.getY() >= 127.0) {
            return 0.5;
        }
        double cap = speedCap.getValue();
        if (burst.getValue() && mode.getValue() != Mode.CONTROL) {
            cap = Math.max(cap, 3.1);
        }
        if (mode.getValue() == Mode.CONTROL) {
            cap = Math.min(cap, controlSpeed.getValue() * 1.5);
        }
        return cap;
    }
}
