package me.earth.earthhack.impl.modules.player.autowalk;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerTick extends ModuleListener<AutoWalk, TickEvent>
{
    public ListenerTick(AutoWalk module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.player == null || mc.gameSettings == null)
        {
            return;
        }

        if (module.mode.getValue() == AutoWalk.Mode.FORWARD)
        {
            mc.gameSettings.keyBindForward.pressed = true;
        }
        else
        {
            mc.gameSettings.keyBindBack.pressed = true;
        }
    }
}
