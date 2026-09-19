package me.earth.earthhack.impl.modules.movement.glide;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Classic glide: slow controlled falling.
 * Ported from SalHack (GlideModule, via Huzuni).
 */
public class Glide extends Module
{
    public Glide()
    {
        super("Glide", Category.Movement);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Glides down slowly like 2015 clients."));
    }
}
