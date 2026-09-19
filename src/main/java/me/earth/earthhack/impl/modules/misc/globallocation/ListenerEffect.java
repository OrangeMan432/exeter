package me.earth.earthhack.impl.modules.misc.globallocation;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.network.play.server.SPacketEffect;
import net.minecraft.util.text.TextFormatting;

final class ListenerEffect extends
        ModuleListener<GlobalLocation, PacketEvent.Receive<SPacketEffect>>
{
    public ListenerEffect(GlobalLocation module)
    {
        super(module, PacketEvent.Receive.class, SPacketEffect.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketEffect> event)
    {
        SPacketEffect packet = event.getPacket();
        int type = packet.getSoundType();
        if (type == 1023 && module.wither.getValue())
        {
            ChatUtil.sendMessage(TextFormatting.RED
                + "[GlobalLocation] Wither spawned at "
                + packet.getSoundPos());
        }
        else if (type == 1028 && module.dragon.getValue())
        {
            ChatUtil.sendMessage(TextFormatting.DARK_PURPLE
                + "[GlobalLocation] Dragon killed at "
                + packet.getSoundPos());
        }
        else if (type == 1038 && module.endPortal.getValue())
        {
            ChatUtil.sendMessage(TextFormatting.LIGHT_PURPLE
                + "[GlobalLocation] End portal opened at "
                + packet.getSoundPos());
        }
    }
}
