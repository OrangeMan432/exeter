package me.earth.earthhack.impl.modules.movement.horsejump;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerTick extends ModuleListener<HorseJump, TickEvent>
{
    public ListenerTick(HorseJump module)
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

        mc.player.horseJumpPower = 1.0f;
        mc.player.horseJumpPowerCounter = -10;
    }
}
