package me.earth.earthhack.impl.modules.misc.fakepearl;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.client.CPacketPlayer;

final class ListenerSend extends
        ModuleListener<FakePearl, PacketEvent.Send<CPacketPlayer>>
{
    public ListenerSend(FakePearl module)
    {
        super(module, PacketEvent.Send.class, CPacketPlayer.class);
    }

    @Override
    public void invoke(PacketEvent.Send<CPacketPlayer> event)
    {
        if (module.thrownPearlId != -1)
        {
            module.packets.add(event.getPacket());
            event.setCancelled(true);
        }
    }
}
