package me.earth.earthhack.impl.modules.movement.parkour;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerTick extends ModuleListener<Parkour, TickEvent>
{
    public ListenerTick(Parkour module)
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

        if (mc.world.getCollisionBoxes(
                mc.player,
                mc.player.getEntityBoundingBox()
                    .offset(0.0, -0.5, 0.0)
                    .expand(0.001, 0.0, 0.001)).isEmpty()
            && mc.player.onGround
            && !mc.player.isSneaking())
        {
            mc.player.jump();
        }
    }
}
