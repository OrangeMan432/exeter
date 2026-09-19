package me.earth.earthhack.impl.modules.player.portalgodmode;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.client.CPacketConfirmTeleport;

final class ListenerConfirmTeleport extends
        ModuleListener<PortalGodMode, PacketEvent.Send<CPacketConfirmTeleport>>
{
    public ListenerConfirmTeleport(PortalGodMode module)
    {
        super(module, PacketEvent.Send.class, CPacketConfirmTeleport.class);
    }

    @Override
    public void invoke(PacketEvent.Send<CPacketConfirmTeleport> event)
    {
        event.setCancelled(true);
    }
}
