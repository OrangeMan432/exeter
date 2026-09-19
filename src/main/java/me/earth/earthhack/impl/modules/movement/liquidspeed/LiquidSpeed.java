package me.earth.earthhack.impl.modules.movement.liquidspeed;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Fast swim controller for water and lava.
 * Ported from Lemon (LiquidSpeed).
 */
public class LiquidSpeed extends Module
{
    protected final Setting<Double> timerSpeed =
        register(new NumberSetting<>("TimerSpeed", 1.0, 1.0, 2.0));
    protected final Setting<Double> xzWater =
        register(new NumberSetting<>("XZWater", 5.75, 0.01, 8.0));
    protected final Setting<Double> upWater =
        register(new NumberSetting<>("YWater", 2.69, 0.01, 8.0));
    protected final Setting<Double> downWater =
        register(new NumberSetting<>("Y-DownWater", 0.8, 0.01, 8.0));
    protected final Setting<Double> xzBoostWater =
        register(new NumberSetting<>("XZBoostWater", 6.0, 1.0, 8.0));
    protected final Setting<Double> yBoostWater =
        register(new NumberSetting<>("YBoostWater", 2.9, 0.1, 8.0));
    protected final Setting<Double> xzLava =
        register(new NumberSetting<>("XZLava", 3.8, 0.01, 8.0));
    protected final Setting<Double> upLava =
        register(new NumberSetting<>("YLava", 2.69, 0.01, 8.0));
    protected final Setting<Double> downLava =
        register(new NumberSetting<>("Y-DownLava", 4.22, 0.01, 8.0));
    protected final Setting<Double> xzBoostLava =
        register(new NumberSetting<>("XZBoostLava", 4.0, 1.0, 8.0));
    protected final Setting<Double> yBoostLava =
        register(new NumberSetting<>("YBoostLava", 2.0, 0.1, 8.0));
    protected final Setting<Double> jitter =
        register(new NumberSetting<>("Jitter", 1.0, 1.0, 20.0));
    protected final Setting<Boolean> groundIgnore =
        register(new BooleanSetting("GroundIgnore", true));

    double moveSpeed;
    double motionY;

    public LiquidSpeed()
    {
        super("LiquidSpeed", Category.Movement);
        this.listeners.add(new ListenerMove(this));
        this.listeners.add(new ListenerPosLook(this));
        this.setData(new SimpleData(this,
            "Swim fast in water and lava."));
    }

    @Override
    protected void onDisable()
    {
        moveSpeed = 0.0;
        motionY = 0.0;
        me.earth.earthhack.impl.managers.Managers.TIMER.reset();
    }
}
