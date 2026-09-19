package me.earth.earthhack.impl.modules.movement.elytraplus;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Direct-control elytra flight, Wasp and Control modes.
 * Ported from BlackOut (ElytraFlyPlus by OLEPOSSU).
 */
public class ElytraPlus extends Module
{
    protected final Setting<Mode> mode =
        register(new EnumSetting<>("Mode", Mode.Wasp));
    protected final Setting<Boolean> stopWater =
        register(new BooleanSetting("StopWater", true));
    protected final Setting<Boolean> stopLava =
        register(new BooleanSetting("StopLava", true));
    protected final Setting<Double> horizontal =
        register(new NumberSetting<>("Horizontal", 1.0, 0.0, 5.0));
    protected final Setting<Double> up =
        register(new NumberSetting<>("Up", 1.0, 0.0, 5.0));
    protected final Setting<Double> speed =
        register(new NumberSetting<>("Speed", 1.0, 0.0, 5.0));
    protected final Setting<Double> upMultiplier =
        register(new NumberSetting<>("UpMultiplier", 1.0, 0.0, 5.0));
    protected final Setting<Double> down =
        register(new NumberSetting<>("Down", 1.0, 0.0, 5.0));
    protected final Setting<Boolean> smartFall =
        register(new BooleanSetting("SmartFall", true));
    protected final Setting<Double> fallSpeed =
        register(new NumberSetting<>("FallSpeed", 0.01, 0.0, 1.0));

    boolean moving;
    float yaw;
    float pitch;
    float p;
    double velocity;

    public ElytraPlus()
    {
        super("ElytraPlus", Category.Movement);
        this.listeners.add(new ListenerMove(this));
        this.setData(new SimpleData(this,
            "Wasp and Control elytra flight."));
    }

    public enum Mode
    {
        Wasp,
        Control
    }
}
