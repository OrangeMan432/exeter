package me.earth.earthhack.impl.modules.render.hitspheres;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

import java.awt.*;

/**
 * Range spheres around players, green far and red close.
 * Ported from GameSense (HitSpheres).
 */
public class HitSpheres extends Module
{
    protected final Setting<Double> range =
        register(new NumberSetting<>("Range", 100.0, 10.0, 260.0));
    protected final Setting<Float> lineWidth =
        register(new NumberSetting<>("LineWidth", 2.0f, 1.0f, 5.0f));
    protected final Setting<Color> farColor =
        register(new ColorSetting("Far", new Color(0, 255, 0, 255)));
    protected final Setting<Color> nearColor =
        register(new ColorSetting("Near", new Color(255, 0, 0, 255)));

    public HitSpheres()
    {
        super("HitSpheres", Category.Render);
        this.listeners.add(new ListenerRender(this));
        this.setData(new SimpleData(this,
            "Spheres showing who is in crystal range."));
    }
}
