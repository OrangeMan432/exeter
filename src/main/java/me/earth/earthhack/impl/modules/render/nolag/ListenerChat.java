package me.earth.earthhack.impl.modules.render.nolag;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.text.TextFormatting;

final class ListenerChat extends
        ModuleListener<NoLag, PacketEvent.Receive<SPacketChat>>
{
    public ListenerChat(NoLag module)
    {
        super(module, PacketEvent.Receive.class, SPacketChat.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketChat> event)
    {
        if (!module.antiSpam.getValue())
        {
            return;
        }

        String chat =
            event.getPacket().chatComponent.getUnformattedText();
        if (chat.contains("㬁") || chat.contains("㠁")
            || chat.contains("䌁") || chat.contains("䐁")
            || chat.contains("ᬁ") || chat.contains("ሁ")
            || chat.contains("ā") || chat.contains("䬁"))
        {
            event.setCancelled(true);
            ChatUtil.sendMessage(TextFormatting.RED
                + "[NoLag] Removed spam text.");
        }
    }
}
