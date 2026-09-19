package me.earth.earthhack.impl.modules.render.coordinates;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerRender2D
        extends ModuleListener<Coordinates, Render2DEvent>
{
    public ListenerRender2D(Coordinates module)
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

        int x = (int) mc.player.posX;
        int y = (int) mc.player.posY;
        int z = (int) mc.player.posZ;
        String text = "XYZ " + x + ", " + y + ", " + z;
        if (module.nether.getValue())
        {
            boolean hell = mc.world.getBiome(
                mc.player.getPosition()).getBiomeName().equals("Hell");
            float factor = hell ? 8.0f : 0.125f;
            text += " [" + (int) (mc.player.posX * factor)
                + ", " + (int) (mc.player.posZ * factor) + "]";
        }

        mc.fontRenderer.drawStringWithShadow(
            text, module.x.getValue(), module.y.getValue(), 0xFFFFFF);
    }
}
