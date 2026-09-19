package me.earth.earthhack.impl.modules.movement.antivoid;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Catches you over the void by zeroing vertical motion.
 * Ported from Mio 0.6.9 (AntiVoid).
 */
public class AntiVoid extends Module
{
    protected final Setting<Integer> height =
        register(new NumberSetting<>("Height", 100, 0, 256));

    public AntiVoid()
    {
        super("AntiVoid", Category.Movement);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Stops vertical motion while falling into the void."));
    }
}
