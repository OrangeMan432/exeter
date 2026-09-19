package me.earth.earthhack.impl.modules.misc.unfocusedcpu;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import org.lwjgl.opengl.Display;

final class ListenerTick extends ModuleListener<UnfocusedCPU, TickEvent>
{
    public ListenerTick(UnfocusedCPU module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.gameSettings == null)
        {
            return;
        }

        if (!Display.isActive())
        {
            mc.gameSettings.limitFramerate = module.fps.getValue();
        }
    }
}
