package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;

/**
 * NoWeather. Hides rain/snow client-side (5b5t: rain kills crystal visibility and FPS in fights).
 *
 * Implementation: each tick, force the level's client-side rain level to 0. The server's
 * weather state is untouched — this is purely the client's render/ambience view. Thunder and
 * the sky darkening follow the same level field, so they clear too.
 *
 * When disabled, the client resyncs to the server's weather on the next weather packet.
 */
public class NoWeather extends ToggleableModule {

    public NoWeather() {
        super("NoWeather", new String[]{"noweather", "norain"}, ModuleType.RENDER);

        this.listeners.add(new Listener<TickEvent>("no_weather_tick") {
            @Override
            public void call(TickEvent event) {
                LocalPlayer player = minecraft.player;
                if (player == null) return;
                Level level = minecraft.level;
                if (level == null) return;

                if (level.getRainLevel(1.0f) > 0.0f) {
                    level.setRainLevel(0.0f);
                }
                if (level.getThunderLevel(1.0f) > 0.0f) {
                    level.setThunderLevel(0.0f);
                }
            }
        });
    }
}
