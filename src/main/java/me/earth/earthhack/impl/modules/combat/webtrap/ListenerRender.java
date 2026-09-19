package me.earth.earthhack.impl.modules.combat.webtrap;

import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.util.Map;

final class ListenerRender extends ModuleListener<WebTrap, Render3DEvent>
{
    public ListenerRender(WebTrap module)
    {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event)
    {
        if (mc.world == null
            || mc.player == null
            || !module.render.getValue())
        {
            return;
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<BlockPos, Long> entry
                : module.renderBlocks.entrySet())
        {
            if (now - entry.getValue() > 2000
                || mc.world.getBlockState(entry.getKey()).getBlock()
                    == Blocks.AIR)
            {
                module.renderBlocks.remove(entry.getKey());
                continue;
            }

            RenderUtil.renderBox(entry.getKey(),
                                 new Color(200, 200, 200, 255),
                                 1.0f);
        }
    }
}
