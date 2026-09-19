package me.earth.earthhack.impl.modules.misc.entitydesync;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.util.text.TextFormatting;

final class ListenerDestroy extends
        ModuleListener<EntityDesync, PacketEvent.Receive<SPacketDestroyEntities>>
{
    public ListenerDestroy(EntityDesync module)
    {
        super(module, PacketEvent.Receive.class, SPacketDestroyEntities.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketDestroyEntities> event)
    {
        if (module.riding == null)
        {
            return;
        }

        for (int id : event.getPacket().getEntityIDs())
        {
            if (id == module.riding.getEntityId())
            {
                ChatUtil.sendMessage(TextFormatting.RED
                    + "[EntityDesync] Ridden entity destroyed.");
                module.riding = null;
                break;
            }
        }
    }
}
