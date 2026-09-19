package me.earth.earthhack.impl.modules.render.coordinates;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * On-screen XYZ with nether conversion.
 * Ported from GameSense (Coordinates HUD).
 */
public class Coordinates extends Module
{
    protected final Setting<Boolean> nether =
        register(new BooleanSetting("Nether", true));
    protected final Setting<Integer> x =
        register(new NumberSetting<>("X", 2, 0, 2000));
    protected final Setting<Integer> y =
        register(new NumberSetting<>("Y", 2, 0, 1200));

    public Coordinates()
    {
        super("CoordsHUD", Category.Render);
        this.listeners.add(new ListenerRender2D(this));
        this.setData(new SimpleData(this,
            "Shows coordinates with nether conversion."));
    }
}
