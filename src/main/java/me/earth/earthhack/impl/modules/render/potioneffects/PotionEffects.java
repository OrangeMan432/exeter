package me.earth.earthhack.impl.modules.render.potioneffects;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Lists your active potion effects.
 * Ported from GameSense (PotionEffects HUD).
 */
public class PotionEffects extends Module
{
    protected final Setting<Integer> x =
        register(new NumberSetting<>("X", 2, 0, 2000));
    protected final Setting<Integer> y =
        register(new NumberSetting<>("Y", 80, 0, 1200));

    public PotionEffects()
    {
        super("PotionEffects", Category.Render);
        this.listeners.add(new ListenerRender2D(this));
        this.setData(new SimpleData(this,
            "Lists active potion effects."));
    }
}
