package me.earth.earthhack.impl.modules.misc.peek;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Shows shulker contents held by other players.
 * Ported from Mio 0.6.9 (Peek), texture-free rendering.
 */
public class Peek extends Module
{
    public Peek()
    {
        super("Peek", Category.Misc);
        this.listeners.add(new ListenerRender2D(this));
        this.setData(new SimpleData(this,
            "See inside enemy shulkers."));
    }
}
