package me.earth.earthhack.impl.modules.movement.strafe;

import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerMotion extends ModuleListener<Strafe, MotionUpdateEvent>
{
    public ListenerMotion(Strafe module)
    {
        super(module, MotionUpdateEvent.class);
    }

    @Override
    public void invoke(MotionUpdateEvent event)
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        switch (event.getStage())
        {
            case PRE:
                module.lastDist = Math.sqrt(
                    (mc.player.posX - mc.player.prevPosX)
                        * (mc.player.posX - mc.player.prevPosX)
                        + (mc.player.posZ - mc.player.prevPosZ)
                            * (mc.player.posZ - mc.player.prevPosZ));
                break;
            default:
                break;
        }
    }
}
