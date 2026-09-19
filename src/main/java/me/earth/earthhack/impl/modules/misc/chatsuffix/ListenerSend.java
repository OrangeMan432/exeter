package me.earth.earthhack.impl.modules.misc.chatsuffix;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.client.CPacketChatMessage;

final class ListenerSend extends
        ModuleListener<ChatSuffix, PacketEvent.Send<CPacketChatMessage>>
{
    public ListenerSend(ChatSuffix module)
    {
        super(module, PacketEvent.Send.class, CPacketChatMessage.class);
    }

    @Override
    public void invoke(PacketEvent.Send<CPacketChatMessage> event)
    {
        String message = event.getPacket().getMessage();
        if (message.startsWith("/")
            || message.startsWith("!")
            || message.endsWith("| exeter")
            || message.endsWith("| 2b2t"))
        {
            return;
        }

        event.setCancelled(true);
        mc.player.connection.sendPacket(new CPacketChatMessage(
            message + (module.suffix2b.getValue() ? " | 2b2t" : " | exeter")));
    }
}
