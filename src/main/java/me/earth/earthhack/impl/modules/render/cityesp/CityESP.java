package me.earth.earthhack.impl.modules.render.cityesp;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

import java.awt.*;

/**
 * Highlights mineable obsidian around holes enemies sit in.
 * Original Exeter addition for the 1.12.2 branch.
 */
public class CityESP extends Module
{
    protected final Setting<Double> range =
        register(new NumberSetting<>("Range", 8.0, 0.0, 16.0));
    protected final Setting<Color> cityColor =
        register(new ColorSetting("City-Color", new Color(255, 120, 0, 255)));
    protected final Setting<Float> height =
        register(new NumberSetting<>("Height", 1.0f, 0.0f, 1.0f));
    protected final Setting<Boolean> self =
        register(new BooleanSetting("Self", false));

    public CityESP()
    {
        super("CityESP", Category.Render);
        this.listeners.add(new ListenerRender(this));
        this.setData(new SimpleData(this,
            "Highlights blocks you can mine to city enemies."));
    }
}
