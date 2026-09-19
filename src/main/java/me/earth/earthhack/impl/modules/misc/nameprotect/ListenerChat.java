package me.earth.earthhack.impl.modules.misc.nameprotect;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.text.TextComponentString;

final class ListenerChat extends
        ModuleListener<NameProtect, PacketEvent.Receive<SPacketChat>>
{
    public ListenerChat(NameProtect module)
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

        String ownName = mc.getSession().getUsername();
        String text =
            event.getPacket().chatComponent.getUnformattedText();
        if (text.contains(ownName))
        {
            event.getPacket().chatComponent = new TextComponentString(
                text.replace(ownName, module.name.getValue()));
        }
    }
}
