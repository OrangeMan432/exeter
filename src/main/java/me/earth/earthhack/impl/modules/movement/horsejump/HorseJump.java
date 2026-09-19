package me.earth.earthhack.impl.modules.movement.horsejump;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Max horse jump charge always.
 * Ported from SalHack (HorseJump).
 */
public class HorseJump extends Module
{
    public HorseJump()
    {
        super("HorseJump", Category.Movement);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Full horse jumps every time."));
    }
}
