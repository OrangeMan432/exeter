package me.earth.earthhack.impl.modules.misc.nokick;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketSoundEffect;

final class ListenerSound extends
        ModuleListener<NoKick, PacketEvent.Receive<SPacketSoundEffect>>
{
    public ListenerSound(NoKick module)
    {
        super(module, PacketEvent.Receive.class, SPacketSoundEffect.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketSoundEffect> event)
    {
        if (module.noOffhandCrash.getValue()
            && event.getPacket().getSound()
                == SoundEvents.ITEM_ARMOR_EQUIP_GENERIC)
        {
            event.setCancelled(true);
        }
    }
}
