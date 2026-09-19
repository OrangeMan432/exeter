package me.earth.earthhack.impl.modules.combat.targetstrafe;

import me.earth.earthhack.impl.event.events.movement.MoveEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerMove extends ModuleListener<TargetStrafe, MoveEvent>
{
    public ListenerMove(TargetStrafe module)
    {
        super(module, MoveEvent.class);
    }

    @Override
    public void invoke(MoveEvent event)
    {
        if (mc.player == null
            || mc.world == null
            || module.target == null
            || module.target.isDead)
        {
            return;
        }

        double dx = mc.player.posX - module.target.posX;
        double dz = mc.player.posZ - module.target.posZ;
        double dist = Math.hypot(dx, dz);
        if (dist < 0.1)
        {
            return;
        }

        double angle = Math.atan2(dz, dx);
        angle += (module.clockwise.getValue() ? 1 : -1) * Math.PI / 2.0;
        double radial = (module.range.getValue() - dist) * 0.2;
        double speed = module.speed.getValue();
        event.setX(Math.cos(angle) * speed + dx / dist * radial);
        event.setZ(Math.sin(angle) * speed + dz / dist * radial);

        if (module.jump.getValue() && mc.player.onGround)
        {
            mc.player.jump();
        }
    }
}
