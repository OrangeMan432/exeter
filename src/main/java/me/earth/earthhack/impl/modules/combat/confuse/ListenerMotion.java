package me.earth.earthhack.impl.modules.combat.confuse;

import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;

final class ListenerMotion extends ModuleListener<Confuse, MotionUpdateEvent>
{
    public ListenerMotion(Confuse module)
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
                break;
            default:
                return;
        }

        module.yaw += module.spinSpeed.getValue();
        if (module.yaw > 180.0f)
        {
            module.yaw -= 360.0f;
        }

        Managers.ROTATION.setServerRotations(
            module.yaw, mc.player.rotationPitch);
    }
}
