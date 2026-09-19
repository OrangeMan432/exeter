package me.earth.earthhack.impl.modules.movement.liquidspeed;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.network.play.server.SPacketPlayerPosLook;

final class ListenerPosLook extends
        ModuleListener<LiquidSpeed, PacketEvent.Receive<SPacketPlayerPosLook>>
{
    public ListenerPosLook(LiquidSpeed module)
    {
        super(module, PacketEvent.Receive.class, SPacketPlayerPosLook.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketPlayerPosLook> event)
    {
        module.moveSpeed = 0.0;
        module.motionY = 0.0;
        Managers.TIMER.reset();
    }
}
