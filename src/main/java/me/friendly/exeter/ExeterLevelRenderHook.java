package me.friendly.exeter;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.LevelRenderEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

/**
 * Fabric level-render hook for 26.2.
 *
 * Wires {@code LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES} into the Exeter event bus as a
 * {@link LevelRenderEvent}. This fires after translucent world features — the right place for
 * world-space overlays (ESP boxes, tracers) that must draw on top of terrain but under the HUD.
 * GL is still in the world frame at this point, so raw GL drawing works; the event carries the
 * camera position for camera-relative math, plus the raw context for modules that need the
 * projection matrices.
 *
 * Registered via Fabric's {@code client} entrypoint (declared in fabric.mod.json alongside
 * {@link ExeterClientTickHook}).
 */
public class ExeterLevelRenderHook implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(ctx -> {
            Exeter exeter = Exeter.getInstance();
            if (exeter == null) return;
            var state = ctx.levelState();
            if (state == null || state.cameraRenderState == null || state.cameraRenderState.pos == null) return;
            exeter.getEventManager().dispatch(new LevelRenderEvent(state.cameraRenderState.pos, ctx));
        });
    }
}
