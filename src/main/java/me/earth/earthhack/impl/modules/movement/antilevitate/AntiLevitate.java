package me.earth.earthhack.impl.modules.movement.antilevitate;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Removes shulker levitation instantly.
 * Ported from Phobos 1.9 (AntiLevitate).
 */
public class AntiLevitate extends Module
{
    public AntiLevitate()
    {
        super("AntiLevitate", Category.Movement);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Clears levitation effect."));
    }
}
