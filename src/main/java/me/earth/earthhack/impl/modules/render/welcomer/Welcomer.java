package me.earth.earthhack.impl.modules.render.welcomer;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.api.setting.settings.StringSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * On-screen welcome text.
 * Ported from GameSense (Welcomer HUD).
 */
public class Welcomer extends Module
{
    protected final Setting<String> text =
        register(new StringSetting("Text", "Welcome "));
    protected final Setting<Integer> x =
        register(new NumberSetting<>("X", 2, 0, 2000));
    protected final Setting<Integer> y =
        register(new NumberSetting<>("Y", 22, 0, 1200));

    public Welcomer()
    {
        super("Welcomer", Category.Render);
        this.listeners.add(new ListenerRender2D(this));
        this.setData(new SimpleData(this,
            "Greets you on screen."));
    }
}
