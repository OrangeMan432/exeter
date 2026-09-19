package me.earth.earthhack.impl.modules.misc.popnotify;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

import java.util.HashMap;
import java.util.Map;

/**
 * Counts other players totem pops and reports deaths after pops.
 * Ported from Mio 0.6.9 (PopNotify).
 */
public class PopNotify extends Module
{
    final Map<String, Integer> totemPops = new HashMap<>();

    public PopNotify()
    {
        super("PopNotify", Category.Misc);
        this.listeners.add(new ListenerPop(this));
        this.listeners.add(new ListenerDeath(this));
        this.setData(new SimpleData(this,
            "Counts other players totem pops and reports deaths."));
    }

    @Override
    protected void onEnable()
    {
        totemPops.clear();
    }
}
