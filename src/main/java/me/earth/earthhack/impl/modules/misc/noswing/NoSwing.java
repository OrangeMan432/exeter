package me.earth.earthhack.impl.modules.misc.noswing;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Stops swing animation packets.
 * Ported from GameSense (NoSwing).
 */
public class NoSwing extends Module
{
    public NoSwing()
    {
        super("NoSwing", Category.Misc);
        this.listeners.add(new ListenerSend(this));
        this.setData(new SimpleData(this,
            "Cancels arm swing packets."));
    }
}
