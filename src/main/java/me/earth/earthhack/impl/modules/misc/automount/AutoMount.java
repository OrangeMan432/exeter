package me.earth.earthhack.impl.modules.misc.automount;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.math.StopWatch;

/**
 * Auto-mounts nearby boats and animals.
 * Ported from SalHack (AutoMount).
 */
public class AutoMount extends Module
{
    protected final Setting<Boolean> boats =
        register(new BooleanSetting("Boats", true));
    protected final Setting<Boolean> horses =
        register(new BooleanSetting("Horses", true));
    protected final Setting<Boolean> donkeys =
        register(new BooleanSetting("Donkeys", true));
    protected final Setting<Boolean> pigs =
        register(new BooleanSetting("Pigs", true));
    protected final Setting<Boolean> llamas =
        register(new BooleanSetting("Llamas", true));
    protected final Setting<Integer> range =
        register(new NumberSetting<>("Range", 4, 0, 10));
    protected final Setting<Float> delay =
        register(new NumberSetting<>("Delay", 1.0f, 0.0f, 10.0f));

    final StopWatch timer = new StopWatch();

    public AutoMount()
    {
        super("AutoMount", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Mounts nearby boats and animals."));
    }
}
