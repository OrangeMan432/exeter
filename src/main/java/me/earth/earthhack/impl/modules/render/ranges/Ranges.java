package me.earth.earthhack.impl.modules.render.ranges;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Draws your place and break range circle.
 * Ported from Phobos 1.9 (Ranges).
 */
public class Ranges extends Module
{
    protected final Setting<Boolean> circle =
        register(new BooleanSetting("Circle", true));
    protected final Setting<Double> radius =
        register(new NumberSetting<>("Radius", 4.5, 0.1, 8.0));
    protected final Setting<Float> lineWidth =
        register(new NumberSetting<>("LineWidth", 1.5f, 0.1f, 5.0f));

    public Ranges()
    {
        super("Ranges", Category.Render);
        this.listeners.add(new ListenerRender(this));
        this.setData(new SimpleData(this,
            "Draws a circle showing your reach."));
    }
}
