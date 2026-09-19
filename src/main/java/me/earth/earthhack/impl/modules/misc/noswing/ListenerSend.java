package me.earth.earthhack.impl.modules.misc.noswing;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.client.CPacketAnimation;

final class ListenerSend extends
        ModuleListener<NoSwing, PacketEvent.Send<CPacketAnimation>>
{
    public ListenerSend(NoSwing module)
    {
        super(module, PacketEvent.Send.class, CPacketAnimation.class);
    }

    @Override
    public void invoke(PacketEvent.Send<CPacketAnimation> event)
    {
        event.setCancelled(true);
    }
}
