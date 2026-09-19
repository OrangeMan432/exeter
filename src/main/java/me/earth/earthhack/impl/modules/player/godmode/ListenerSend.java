package me.earth.earthhack.impl.modules.player.godmode;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.client.CPacketPlayer;

final class ListenerSend extends
        ModuleListener<Godmode, PacketEvent.Send<CPacketPlayer>>
{
    public ListenerSend(Godmode module)
    {
        super(module, PacketEvent.Send.class, CPacketPlayer.class);
    }

    @Override
    public void invoke(PacketEvent.Send<CPacketPlayer> event)
    {
        if (module.entity == null)
        {
            return;
        }

        if (event.getPacket() instanceof CPacketPlayer.Position
            || event.getPacket()
                instanceof CPacketPlayer.PositionRotation)
        {
            event.setCancelled(true);
        }
    }
}
