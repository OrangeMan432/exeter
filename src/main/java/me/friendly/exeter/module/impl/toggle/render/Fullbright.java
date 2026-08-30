package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import net.minecraft.client.OptionInstance;

/**
 * Fullbright. Maxes the game's brightness so caves/nether roofs on 5b5t are readable without
 * night vision potions.
 *
 * Ported from Exeter 1.12.2 Fullbright (gamma mode). The old GammaSettingEvent/NightVisionEvent
 * hooks have no dispatchers in this base, so the port writes the vanilla brightness OptionInstance
 * directly every tick while enabled, and restores the player's original value on disable.
 *
 * Modes:
 *  - Gamma : sets vanilla brightness to maximum (100%). Works everywhere, saves to options.txt.
 *  - Legit : sets brightness to a high-but-vanilla-allowed value (default slider max is 100%,
 *            so this matches Gamma on modern versions; kept for parity with the 1.12 module).
 */
public class Fullbright extends ToggleableModule {
    private enum Mode { GAMMA, LEGIT }

    private final EnumProperty<Mode> mode = new EnumProperty<>(Mode.GAMMA, "Mode", "m");
    private Double originalGamma = null;

    public Fullbright() {
        super("Fullbright", new String[]{"fullbright", "bright", "brightness", "fb"}, ModuleType.RENDER);
        offerProperties(mode);

        this.listeners.add(new Listener<TickEvent>("fullbright_tick") {
            @Override
            public void call(TickEvent event) {
                if (minecraft.options == null) return;
                OptionInstance<Double> gamma = minecraft.options.gamma();
                if (gamma == null) return;

                if (originalGamma == null) {
                    originalGamma = gamma.get();
                }
                double target = mode.getValue() == Mode.LEGIT ? 1.0 : 16.0;
                if (gamma.get() < target) {
                    gamma.set(target);
                }
            }
        });
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        if (originalGamma != null && minecraft.options != null) {
            OptionInstance<Double> gamma = minecraft.options.gamma();
            if (gamma != null) {
                gamma.set(Math.min(originalGamma, 1.0));
            }
        }
        originalGamma = null;
    }
}
