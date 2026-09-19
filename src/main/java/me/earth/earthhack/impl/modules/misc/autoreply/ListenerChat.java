package me.earth.earthhack.impl.modules.misc.autoreply;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.server.SPacketChat;

final class ListenerChat extends
        ModuleListener<AutoReply, PacketEvent.Receive<SPacketChat>>
{
    public ListenerChat(AutoReply module)
    {
        super(module, PacketEvent.Receive.class, SPacketChat.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketChat> event)
    {
        if (mc.player == null)
        {
            return;
        }

        String text =
            event.getPacket().chatComponent.getUnformattedText();
        if (text.contains("whispers: ")
            && !text.startsWith(mc.player.getName())
            && !text.contains(module.reply.getValue()))
        {
            mc.player.sendChatMessage("/r " + module.reply.getValue());
        }
    }
}
