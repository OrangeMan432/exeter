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
            || message.endsWith(module.suffix.getValue()))
        {
            return;
        }

        String text = module.fancy.getValue()
            ? toFancy(message)
            : message;
        event.setCancelled(true);
        mc.player.connection.sendPacket(new CPacketChatMessage(
            text + module.suffix.getValue()));
    }

    private String toFancy(String message)
    {
        StringBuilder builder = new StringBuilder(message.length());
        for (int i = 0; i < message.length(); i++)
        {
            char c = message.charAt(i);
            if (c >= 'a' && c <= 'z')
            {
                builder.append((char) (c - 'a' + 0xFF41));
            }
            else if (c >= 'A' && c <= 'Z')
            {
                builder.append((char) (c - 'A' + 0xFF21));
            }
            else if (c >= '0' && c <= '9')
            {
                builder.append((char) (c - '0' + 0xFF10));
            }
            else
            {
                builder.append(c);
            }
        }

        return builder.toString();
    }
}
