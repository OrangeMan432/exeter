package me.earth.earthhack.impl.modules.player.antiforcelook;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Blocks server forced look angles.
 * Ported from Kami Blue (AntiForceLook).
 */
public class AntiForceLook extends Module
{
    public AntiForceLook()
    {
        super("AntiForceLook", Category.Player);
        this.listeners.add(new ListenerPosLook(this));
        this.setData(new SimpleData(this,
            "Stops the server turning your head."));
    }
}
