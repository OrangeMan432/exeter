package me.earth.earthhack.impl.modules.misc.killeffects;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.math.StopWatch;

/**
 * Lightning and sounds on kills.
 * Ported from Mio 0.6.9 (KillEffects).
 */
public class KillEffects extends Module
{
    protected final Setting<Lightning> lightning =
        register(new EnumSetting<>("Lightning", Lightning.NORMAL));
    protected final Setting<KillSound> killSound =
        register(new EnumSetting<>("KillSound", KillSound.OFF));

    final StopWatch timer = new StopWatch();

    public KillEffects()
    {
        super("KillEffects", Category.Misc);
        this.listeners.add(new ListenerDeath(this));
        this.setData(new SimpleData(this,
            "Strikes lightning on your kills."));
    }

    public enum Lightning
    {
        NORMAL,
        SILENT,
        OFF
    }

    public enum KillSound
    {
        HYPIXEL,
        OFF
    }
}
