package me.friendly.exeter;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Fabric client-tick hook.
 *
 * The Exeter event bus has a {@link TickEvent} and every tick-based module registers a
 * {@code Listener<TickEvent>}, but nothing ever dispatched it — every tick module was dead
 * code. This hook wires Fabric's {@code ClientTickEvents.END_CLIENT_TICK} to the event bus,
 * so all tick listeners fire once per client tick, exactly like the Forge-era tick loop did.
 *
 * Registered via Fabric's {@code client} entrypoint (declared in fabric.mod.json).
 */
public class ExeterClientTickHook implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Exeter exeter = Exeter.getInstance();
            if (exeter == null) return;
            exeter.getEventManager().dispatch(new TickEvent());
        });
    }
}
