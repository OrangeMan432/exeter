package me.earth.earthhack.impl.modules.misc.entitydesync;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.SPacketSetPassengers;
import net.minecraft.util.text.TextFormatting;

final class ListenerPassengers extends
        ModuleListener<EntityDesync, PacketEvent.Receive<SPacketSetPassengers>>
{
    public ListenerPassengers(EntityDesync module)
    {
        super(module, PacketEvent.Receive.class, SPacketSetPassengers.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketSetPassengers> event)
    {
        if (module.riding == null || mc.world == null)
        {
            return;
        }

        Entity entity =
            mc.world.getEntityByID(event.getPacket().getEntityId());
        if (entity != module.riding)
        {
            return;
        }

        for (int id : event.getPacket().getPassengerIds())
        {
            if (mc.world.getEntityByID(id) == mc.player)
            {
                return;
            }
        }

        ChatUtil.sendMessage(TextFormatting.RED
            + "[EntityDesync] You dismounted, disabling.");
        module.disable();
    }
}
