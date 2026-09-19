package me.earth.earthhack.impl.modules.misc.fakepearl;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.network.play.client.CPacketPlayer;

final class ListenerTick extends ModuleListener<FakePearl, TickEvent>
{
    public ListenerTick(FakePearl module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        if (module.thrownPearlId != -1)
        {
            for (Entity entity : mc.world.loadedEntityList)
            {
                if (entity.getEntityId() == module.thrownPearlId
                    && entity instanceof EntityEnderPearl
                    && entity.isDead)
                {
                    module.thrownPearlId = -1;
                }
            }
        }
        else if (!module.packets.isEmpty())
        {
            CPacketPlayer packet;
            while ((packet = module.packets.poll()) != null)
            {
                mc.player.connection.sendPacket(packet);
            }
        }
    }
}
