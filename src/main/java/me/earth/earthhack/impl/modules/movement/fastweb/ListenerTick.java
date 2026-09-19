package me.earth.earthhack.impl.modules.movement.fastweb;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;

final class ListenerTick extends ModuleListener<FastWeb, TickEvent>
{
    public ListenerTick(FastWeb module)
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

        if (!mc.player.isInWeb)
        {
            Managers.TIMER.reset();
            return;
        }

        if (module.mode.getValue() == FastWeb.Mode.FAST
            && mc.gameSettings.keyBindSneak.isKeyDown())
        {
            Managers.TIMER.reset();
            mc.player.motionY -= module.fastSpeed.getValue();
        }
        else if (module.mode.getValue() == FastWeb.Mode.STRICT
            && !mc.player.onGround
            && mc.gameSettings.keyBindSneak.isKeyDown())
        {
            Managers.TIMER.setTimer(8.0f);
        }
        else
        {
            Managers.TIMER.reset();
        }
    }
}
