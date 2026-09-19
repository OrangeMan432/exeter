package me.earth.earthhack.impl.modules.misc.globallocation;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;

final class ListenerSound extends
        ModuleListener<GlobalLocation, PacketEvent.Receive<SPacketSoundEffect>>
{
    public ListenerSound(GlobalLocation module)
    {
        super(module, PacketEvent.Receive.class, SPacketSoundEffect.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketSoundEffect> event)
    {
        SPacketSoundEffect packet = event.getPacket();
        if (packet.getCategory() != SoundCategory.WEATHER
            || packet.getSound() != SoundEvents.ENTITY_LIGHTNING_THUNDER)
        {
            return;
        }

        if (module.thunder.getValue())
        {
            ChatUtil.sendMessage(TextFormatting.GOLD
                + "[GlobalLocation] Thunder at "
                + (int) packet.getX() + ", "
                + (int) packet.getY() + ", "
                + (int) packet.getZ());
        }
    }
}
