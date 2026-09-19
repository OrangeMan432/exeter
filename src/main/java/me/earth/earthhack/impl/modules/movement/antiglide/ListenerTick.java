package me.earth.earthhack.impl.modules.movement.antiglide;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.util.MovementInput;

final class ListenerTick extends ModuleListener<AntiGlide, TickEvent>
{
    public ListenerTick(AntiGlide module)
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

        if (!module.onGround.getValue() && !mc.player.onGround)
        {
            return;
        }

        MovementInput input = mc.player.movementInput;
        if (input.moveForward == 0.0f && input.moveStrafe == 0.0f)
        {
            mc.player.motionX = 0.0;
            mc.player.motionZ = 0.0;
        }

        if (module.ice.getValue() && mc.player.getRidingEntity() == null)
        {
            AntiGlide.setIceSlipperiness(0.6f);
        }
        else
        {
            AntiGlide.setIceSlipperiness(0.98f);
        }
    }
}
