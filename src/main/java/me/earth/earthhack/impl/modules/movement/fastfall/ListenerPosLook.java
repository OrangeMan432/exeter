package me.earth.earthhack.impl.modules.movement.fastfall;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.server.SPacketPlayerPosLook;

final class ListenerPosLook extends
        ModuleListener<FastFall, PacketEvent.Receive<SPacketPlayerPosLook>>
{
    public ListenerPosLook(FastFall module)
    {
        super(module, PacketEvent.Receive.class, SPacketPlayerPosLook.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketPlayerPosLook> event)
    {
        module.lagTimer.reset();
    }
}
