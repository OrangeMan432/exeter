package me.earth.earthhack.impl.modules.player.fastexp;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.init.Items;

final class ListenerReceive
        extends ModuleListener<FastExp, PacketEvent.Receive<?>>
{
    public ListenerReceive(FastExp module)
    {
        super(module, PacketEvent.Receive.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<?> event)
    {
        if (mc.player != null
            && mc.player.getHeldItemMainhand().getItem()
                == Items.EXPERIENCE_BOTTLE)
        {
            mc.rightClickDelayTimer = 0;
        }
    }
}
