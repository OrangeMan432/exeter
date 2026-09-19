package me.earth.earthhack.impl.modules.render.portal;

import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.util.math.BlockPos;

import java.awt.*;

final class ListenerRender extends ModuleListener<PortalESP, Render3DEvent>
{
    public ListenerRender(PortalESP module)
    {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        for (BlockPos pos : new java.util.ArrayList<>(module.portals))
        {
            Color color = module.color.getValue();
            if (module.box.getValue())
            {
                RenderUtil.renderBox(
                    pos,
                    new Color(color.getRed(),
                              color.getGreen(),
                              color.getBlue(),
                              module.boxAlpha.getValue()),
                    1.0f);
            }

            if (module.outline.getValue())
            {
                RenderUtil.renderBox(
                    new net.minecraft.util.math.AxisAlignedBB(
                        pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
                    new Color(0, 0, 0, 0),
                    color,
                    module.lineWidth.getValue());
            }
        }
    }
}
