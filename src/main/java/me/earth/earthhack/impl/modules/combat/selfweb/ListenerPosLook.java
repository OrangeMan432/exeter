package me.earth.earthhack.impl.modules.combat.selfweb;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.server.SPacketPlayerPosLook;

final class ListenerPosLook extends
        ModuleListener<SelfWeb, PacketEvent.Receive<SPacketPlayerPosLook>>
{
    public ListenerPosLook(SelfWeb module)
    {
        super(module, PacketEvent.Receive.class, SPacketPlayerPosLook.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketPlayerPosLook> event)
    {
        module.lagTimer.reset();
    }
}
