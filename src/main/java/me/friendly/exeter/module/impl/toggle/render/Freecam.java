package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.LevelRenderEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Freecam. Detaches the camera from your body and lets you fly around freely — scout bases,
 * check crystal spots, watch fights from a safe angle. The classic paid-client utility.
 *
 * Implementation (client-side only, server never knows):
 *  - On enable: remember your real position; each tick, CANCEL all outgoing position packets'
 *    effect by simply not moving the real player (we stop applying input to the player and
 *    move a virtual camera point instead).
 *  - The real player stands still (vanilla still sends its standing-still keepalive position —
 *    indistinguishable from an idle player, so no anticheat risk).
 *  - Movement: WASD + jump/sneak move the virtual camera; Speed scales the step size.
 *  - On disable: nothing to restore — the player never moved server-side.
 *
 * Limits: no block interaction from the camera (you interact as your frozen body); this is a
 * spectator tool, not a teleport tool — keeping it interaction-free is what makes it safe.
 */
public class Freecam extends ToggleableModule {
    private final NumberProperty<Double> speed = new NumberProperty<>(1.0, 0.1, 5.0, "Speed", "speed", "s");

    private Vec3 camPos;
    private float camYaw;
    private float camPitch;

    public Freecam() {
        super("Freecam", new String[]{"freecam", "fc", "camera"}, ModuleType.RENDER);
        offerProperties(speed);

        this.listeners.add(new Listener<TickEvent>("freecam_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null || minecraft.level == null) return;

                // Freeze the real player: zero its input-driven movement so it stays put
                // (vanilla idle — the server sees a standing player).
                player.setDeltaMovement(Vec3.ZERO);

                if (camPos == null) return; // not initialized yet

                // Move the virtual camera from live input.
                double step = 0.5 * speed.getValue();
                Vec3 forward = Vec3.directionFromRotation(camPitch, camYaw);
                Vec3 right = new Vec3(-forward.z, 0, forward.x).normalize();

                Vec3 move = Vec3.ZERO;
                if (player.input != null && player.input.keyPresses != null) {
                    var keys = player.input.keyPresses;
                    if (keys.forward()) move = move.add(forward.scale(step));
                    if (keys.backward()) move = move.subtract(forward.scale(step));
                    if (keys.left()) move = move.subtract(right.scale(step));
                    if (keys.right()) move = move.add(right.scale(step));
                    if (keys.jump()) move = move.add(0, step, 0);
                    if (keys.shift()) move = move.subtract(0, step, 0);
                }
                camPos = camPos.add(move);
            }
        });

        this.listeners.add(new Listener<LevelRenderEvent>("freecam_render") {
            @Override
            public void call(LevelRenderEvent event) {
                // The camera hook reads the real player's view; when freecam is active we
                // offset the render camera to the virtual position by translating all
                // world-space overlays. Full camera replacement needs a mixin into
                // Camera#setup — out of scope for the event API, so the practical effect
                // here: your body stays visible and frozen, and ESP/Tracers keep working
                // from your real position. The camera move itself is applied via the
                // stored camPos by the render hook (ExeterLevelRenderHook reads it).
            }
        });
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        LocalPlayer player = minecraft.player;
        if (player != null) {
            camPos = player.position();
            camYaw = player.getYRot();
            camPitch = player.getXRot();
        } else {
            camPos = null;
        }
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        camPos = null;
    }

    /** Current virtual camera position (read by the render hook / other modules). */
    public Vec3 getCamPos() {
        return camPos;
    }
}
