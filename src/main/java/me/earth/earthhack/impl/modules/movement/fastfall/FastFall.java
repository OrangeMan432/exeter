package me.earth.earthhack.impl.modules.movement.fastfall;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.math.StopWatch;

/**
 * Falls faster with timer control and packet flight fall.
 * Ported from Mio 0.6.9 (FastFall).
 */
public class FastFall extends Module
{
    protected final Setting<Mode> mode =
        register(new EnumSetting<>("Mode", Mode.All));
    protected final Setting<Float> fallSpeed =
        register(new NumberSetting<>("FallSpeed", 1.0f, 0.3f, 5.0f));
    protected final Setting<Float> height =
        register(new NumberSetting<>("Height", 4.0f, 3.0f, 10.0f));
    protected final Setting<Boolean> noLag =
        register(new BooleanSetting("NoLag", false));

    final StopWatch lagTimer = new StopWatch();

    public FastFall()
    {
        super("FastFall", Category.Movement);
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerPosLook(this));
        this.setData(new SimpleData(this,
            "Drops you fast with timer and packet options."));
    }

    public enum Mode
    {
        All,
        Timer,
        Packet
    }
}
