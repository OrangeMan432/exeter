package me.earth.earthhack.impl.modules.movement.fastweb;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Moves fast through webs instead of holding a timer key.
 * Ported from Mio 0.6.9 (FastWeb).
 */
public class FastWeb extends Module
{
    protected final Setting<Mode> mode =
        register(new EnumSetting<>("Mode", Mode.FAST));
    protected final Setting<Float> fastSpeed =
        register(new NumberSetting<>("FastSpeed", 3.0f, 0.0f, 5.0f));

    public FastWeb()
    {
        super("FastWeb", Category.Movement);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Timer control for moving through webs."));
    }

    @Override
    protected void onDisable()
    {
        Managers.TIMER.reset();
    }

    public enum Mode
    {
        FAST,
        STRICT
    }
}
