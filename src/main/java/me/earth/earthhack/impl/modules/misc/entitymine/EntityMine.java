package me.earth.earthhack.impl.modules.misc.entitymine;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Mines blocks through entities in the way.
 * Ported from Wurst+3 (EntityMine).
 */
public class EntityMine extends Module
{
    public EntityMine()
    {
        super("EntityMine", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Mine blocks even with entities blocking."));
    }
}
