package me.earth.earthhack.impl.modules.movement.parkour;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Jumps at block edges automatically.
 * Ported from SalHack (ParkourJump, via Wolfram).
 */
public class Parkour extends Module
{
    public Parkour()
    {
        super("Parkour", Category.Movement);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Jumps at the edge of blocks."));
    }
}
