package me.earth.earthhack.impl.modules.player.flagdetect;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.text.TextFormatting;

final class ListenerPosLook extends
        ModuleListener<FlagDetect, PacketEvent.Receive<SPacketPlayerPosLook>>
{
    public ListenerPosLook(FlagDetect module)
    {
        super(module, PacketEvent.Receive.class, SPacketPlayerPosLook.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketPlayerPosLook> event)
    {
        if (mc.world == null
            || mc.player == null
            || !module.chatNotify.getValue())
        {
            return;
        }

        ChatUtil.sendMessage(
            TextFormatting.RED + "Server lagged you back!");
    }
}
