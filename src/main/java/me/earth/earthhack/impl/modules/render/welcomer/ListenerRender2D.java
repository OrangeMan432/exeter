package me.earth.earthhack.impl.modules.render.welcomer;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerRender2D
        extends ModuleListener<Welcomer, Render2DEvent>
{
    public ListenerRender2D(Welcomer module)
    {
        super(module, Render2DEvent.class);
    }

    @Override
    public void invoke(Render2DEvent event)
    {
        if (mc.player == null)
        {
            return;
        }

        mc.fontRenderer.drawStringWithShadow(
            module.text.getValue() + mc.getSession().getUsername(),
            module.x.getValue(),
            module.y.getValue(),
            0xFFFFFF);
    }
}
