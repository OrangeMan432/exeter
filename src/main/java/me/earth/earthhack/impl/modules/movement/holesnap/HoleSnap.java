package me.earth.earthhack.impl.modules.movement.holesnap;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Pulls you into nearby holes while falling.
 * Original Exeter addition for the 1.12.2 branch.
 */
public class HoleSnap extends Module
{
    protected final Setting<Double> range =
        register(new NumberSetting<>("Range", 4.0, 1.0, 8.0));
    protected final Setting<Double> pull =
        register(new NumberSetting<>("Pull", 0.3, 0.05, 1.0));
    protected final Setting<Boolean> stepDown =
        register(new BooleanSetting("StepDown", true));

    public HoleSnap()
    {
        super("HoleSnap", Category.Movement);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Snaps your motion into holes you fall over."));
    }
}
