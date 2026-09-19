package me.earth.earthhack.impl.modules.misc.entitydesync;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.client.CPacketVehicleMove;

final class ListenerTick extends ModuleListener<EntityDesync, TickEvent>
{
    public ListenerTick(EntityDesync module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.world == null
            || mc.player == null
            || module.riding == null
            || mc.player.isRiding())
        {
            return;
        }

        mc.player.onGround = true;
        module.riding.setPosition(mc.player.posX,
                                 mc.player.posY,
                                 mc.player.posZ);
        mc.player.connection.sendPacket(
            new CPacketVehicleMove(module.riding));
    }
}
