package me.earth.earthhack.impl.modules.player.antiforcelook;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.server.SPacketPlayerPosLook;

final class ListenerPosLook extends
        ModuleListener<AntiForceLook, PacketEvent.Receive<SPacketPlayerPosLook>>
{
    public ListenerPosLook(AntiForceLook module)
    {
        super(module, PacketEvent.Receive.class, SPacketPlayerPosLook.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketPlayerPosLook> event)
    {
        if (mc.player == null)
        {
            return;
        }

        event.getPacket().yaw = mc.player.rotationYaw;
        event.getPacket().pitch = mc.player.rotationPitch;
    }
}
