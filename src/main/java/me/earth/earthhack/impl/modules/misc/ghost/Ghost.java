package me.earth.earthhack.impl.modules.misc.ghost;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Ghosts through death until respawn.
 * Ported from SalHack (Ghost).
 */
public class Ghost extends Module
{
    boolean ghosted;

    public Ghost()
    {
        super("Ghost", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerSend(this));
        this.setData(new SimpleData(this,
            "Stays alive as a ghost after dying."));
    }

    @Override
    protected void onDisable()
    {
        ghosted = false;
        if (mc.player != null)
        {
            mc.player.respawnPlayer();
        }
    }
}
