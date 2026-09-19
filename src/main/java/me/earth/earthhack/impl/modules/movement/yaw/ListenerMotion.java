package me.earth.earthhack.impl.modules.movement.yaw;

import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;

final class ListenerMotion extends ModuleListener<Yaw, MotionUpdateEvent>
{
    public ListenerMotion(Yaw module)
    {
        super(module, MotionUpdateEvent.class);
    }

    @Override
    public void invoke(MotionUpdateEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        switch (event.getStage())
        {
            case PRE:
                break;
            default:
                return;
        }

        Entity entity =
            mc.player.isRiding() ? mc.player.getRidingEntity() : mc.player;
        if (entity == null)
        {
            return;
        }

        if (module.yawLock.getValue())
        {
            entity.rotationYaw = module.yaw;
        }

        if (module.pitchLock.getValue())
        {
            entity.rotationPitch = module.pitch;
        }

        if (module.cardinal.getValue())
        {
            entity.rotationYaw =
                Math.round((entity.rotationYaw + 1.0f) / 45.0f) * 45.0f;
        }
    }
}
