package me.earth.earthhack.impl.modules.player.portalgodmode;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Cancels teleport confirmations for portal godmode.
 * Ported from Kami Blue (PortalGodMode).
 */
public class PortalGodMode extends Module
{
    public PortalGodMode()
    {
        super("PortalGodMode", Category.Player);
        this.listeners.add(new ListenerConfirmTeleport(this));
        this.setData(new SimpleData(this,
            "Cancels teleport confirmations, godmode in portals."));
    }
}
