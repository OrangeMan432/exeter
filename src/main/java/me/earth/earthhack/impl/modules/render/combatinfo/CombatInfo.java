package me.earth.earthhack.impl.modules.render.combatinfo;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Nearest enemy name, health and distance.
 * Ported from GameSense (CombatInfo HUD).
 */
public class CombatInfo extends Module
{
    protected final Setting<Double> range =
        register(new NumberSetting<>("Range", 100.0, 10.0, 260.0));
    protected final Setting<Integer> x =
        register(new NumberSetting<>("X", 2, 0, 2000));
    protected final Setting<Integer> y =
        register(new NumberSetting<>("Y", 60, 0, 1200));

    public CombatInfo()
    {
        super("CombatInfo", Category.Render);
        this.listeners.add(new ListenerRender2D(this));
        this.setData(new SimpleData(this,
            "Shows your closest enemy stats."));
    }
}
