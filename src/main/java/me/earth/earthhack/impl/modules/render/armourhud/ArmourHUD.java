package me.earth.earthhack.impl.modules.render.armourhud;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * On-screen armor with durability percents.
 * Ported from GameSense (ArmourHUD).
 */
public class ArmourHUD extends Module
{
    protected final Setting<Boolean> durability =
        register(new BooleanSetting("Durability", true));
    protected final Setting<Integer> x =
        register(new NumberSetting<>("X", 2, 0, 2000));
    protected final Setting<Integer> y =
        register(new NumberSetting<>("Y", 100, 0, 1200));

    public ArmourHUD()
    {
        super("ArmourHUD", Category.Render);
        this.listeners.add(new ListenerRender2D(this));
        this.setData(new SimpleData(this,
            "Shows armor durability on screen."));
    }
}
