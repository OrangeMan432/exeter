package me.earth.earthhack.impl.modules.misc.pearlnotify;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Announces who threw an ender pearl and where it is heading.
 * Ported from Mio 0.6.9 (PearlNotify).
 */
public class PearlNotify extends Module
{
    boolean flag = true;

    public PearlNotify()
    {
        super("PearlNotify", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Notifies when someone throws an ender pearl."));
    }

    @Override
    protected void onEnable()
    {
        flag = true;
    }
}
