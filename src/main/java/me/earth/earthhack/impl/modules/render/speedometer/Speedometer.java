package me.earth.earthhack.impl.modules.render.speedometer;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * On-screen speed in km/h.
 * Ported from GameSense (Speedometer HUD).
 */
public class Speedometer extends Module
{
    protected final Setting<Integer> x =
        register(new NumberSetting<>("X", 2, 0, 2000));
    protected final Setting<Integer> y =
        register(new NumberSetting<>("Y", 12, 0, 1200));

    public Speedometer()
    {
        super("Speedometer", Category.Render);
        this.listeners.add(new ListenerRender2D(this));
        this.setData(new SimpleData(this,
            "Shows your speed."));
    }
}
