package me.earth.earthhack.impl.modules.misc.framedupe;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Item-frame dupe, shulkers preferred.
 * Ported from Lemon (AutoFrameDupe, 6bDupe).
 */
public class FrameDupe extends Module
{
    protected final Setting<Boolean> shulkersOnly =
        register(new BooleanSetting("ShulkersOnly", true));
    protected final Setting<Integer> range =
        register(new NumberSetting<>("Range", 5, 0, 6));
    protected final Setting<Integer> turns =
        register(new NumberSetting<>("Turns", 1, 0, 3));
    protected final Setting<Integer> ticks =
        register(new NumberSetting<>("Ticks", 10, 1, 20));

    int timeout;

    public FrameDupe()
    {
        super("FrameDupe", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Dupes held items through item frames."));
    }

    @Override
    protected void onDisable()
    {
        timeout = 0;
    }
}
