package me.earth.earthhack.impl.modules.misc.ghost;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.client.CPacketPlayer;

final class ListenerSend extends
        ModuleListener<Ghost, PacketEvent.Send<CPacketPlayer>>
{
    public ListenerSend(Ghost module)
    {
        super(module, PacketEvent.Send.class, CPacketPlayer.class);
    }

    @Override
    public void invoke(PacketEvent.Send<CPacketPlayer> event)
    {
        if (module.ghosted)
        {
            event.setCancelled(true);
        }
    }
}
