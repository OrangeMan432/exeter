package me.earth.earthhack.impl.modules.movement.strafe;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.server.SPacketPlayerPosLook;

final class ListenerPosLook extends
        ModuleListener<Strafe, PacketEvent.Receive<SPacketPlayerPosLook>>
{
    public ListenerPosLook(Strafe module)
    {
        super(module, PacketEvent.Receive.class, SPacketPlayerPosLook.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketPlayerPosLook> event)
    {
        if (module.noLag.getValue())
        {
            module.stage = module.mode.getValue() == Strafe.Mode.BHOP
                && (module.bhop.getValue() || module.hop.getValue()) ? 1 : 4;
        }
    }
}
