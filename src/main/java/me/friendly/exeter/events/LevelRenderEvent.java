package me.friendly.exeter.events;

import me.friendly.api.event.Event;
import net.minecraft.world.phys.Vec3;

/**
 * Fired from the Fabric {@code LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES} hook
 * (see {@code ExeterLevelRenderHook}). Carries the camera position for this render frame so
 * world-space overlays (ESP boxes, tracers) can translate into the camera-relative frame, plus
 * a raw-context accessor for modules that need the full render state (projection matrices).
 */
public class LevelRenderEvent extends Event {
    private final Vec3 cameraPos;
    private final Object rawContext;

    public LevelRenderEvent(Vec3 cameraPos, Object rawContext) {
        this.cameraPos = cameraPos;
        this.rawContext = rawContext;
    }

    public Vec3 getCameraPos() {
        return this.cameraPos;
    }

    /** The Fabric {@code LevelRenderContext}, typed loosely to keep the event API MC-independent. */
    public Object getContextRaw() {
        return this.rawContext;
    }
}
