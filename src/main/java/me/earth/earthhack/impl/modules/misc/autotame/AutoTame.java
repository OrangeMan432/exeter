package me.earth.earthhack.impl.modules.misc.autotame;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.math.StopWatch;
import net.minecraft.entity.passive.AbstractHorse;

/**
 * Remounts untamed horses until they tame.
 * Ported from SalHack (AutoTame).
 */
public class AutoTame extends Module
{
    protected final Setting<Float> delay =
        register(new NumberSetting<>("Delay", 0.1f, 0.0f, 1.0f));
    protected final Setting<Integer> range =
        register(new NumberSetting<>("Range", 4, 0, 10));

    final StopWatch timer = new StopWatch();
    AbstractHorse target;

    public AutoTame()
    {
        super("AutoTame", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Tames horses by remounting them."));
    }

    @Override
    protected void onDisable()
    {
        target = null;
    }
}
