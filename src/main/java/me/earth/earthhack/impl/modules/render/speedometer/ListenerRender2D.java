package me.earth.earthhack.impl.modules.render.speedometer;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerRender2D
        extends ModuleListener<Speedometer, Render2DEvent>
{
    public ListenerRender2D(Speedometer module)
    {
        super(module, Render2DEvent.class);
    }

    @Override
    public void invoke(Render2DEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        double dist = Math.hypot(mc.player.posX - mc.player.prevPosX,
                                 mc.player.posZ - mc.player.prevPosZ);
        String text =
            String.format("%.1f km/h", dist * 20.0 * 3.6);
        mc.fontRenderer.drawStringWithShadow(
            text, module.x.getValue(), module.y.getValue(), 0xFFFFFF);
    }
}
