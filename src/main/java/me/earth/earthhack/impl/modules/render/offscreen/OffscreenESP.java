package me.earth.earthhack.impl.modules.render.offscreen;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

import java.awt.*;

/**
 * Arrows pointing at offscreen players.
 * Ported from Phobos 1.9 (OffscreenESP/ArrowESP), simplified.
 */
public class OffscreenESP extends Module
{
    protected final Setting<Boolean> invisibles =
        register(new BooleanSetting("Invisibles", false));
    protected final Setting<Double> range =
        register(new NumberSetting<>("Range", 100.0, 10.0, 260.0));
    protected final Setting<Integer> radius =
        register(new NumberSetting<>("Radius", 45, 10, 200));
    protected final Setting<Float> size =
        register(new NumberSetting<>("Size", 10.0f, 5.0f, 25.0f));
    protected final Setting<Color> color =
        register(new ColorSetting("Color", new Color(255, 0, 255, 255)));

    public OffscreenESP()
    {
        super("OffscreenESP", Category.Render);
        this.listeners.add(new ListenerRender2D(this));
        this.setData(new SimpleData(this,
            "Triangles pointing at players off screen."));
    }
}
