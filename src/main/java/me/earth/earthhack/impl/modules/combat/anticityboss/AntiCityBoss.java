package me.earth.earthhack.impl.modules.combat.anticityboss;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Extends your hole so enemies cannot city you.
 * Ported from SalHack (AntiCityBoss).
 */
public class AntiCityBoss extends Module
{
    protected final Setting<Boolean> trapCheck =
        register(new BooleanSetting("TrapCheck", false));
    protected final Setting<Double> range =
        register(new NumberSetting<>("Range", 6.0, 1.0, 12.0));
    protected final Setting<Integer> blocksPerTick =
        register(new NumberSetting<>("BPT", 4, 1, 8));

    public AntiCityBoss()
    {
        super("AntiCityBoss", Category.Combat);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Walls off city attempts around you."));
    }
}
