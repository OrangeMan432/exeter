package me.earth.earthhack.impl.modules.render.breakingesp;

import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.util.Map;

final class ListenerRender extends ModuleListener<BreakingESP, Render3DEvent>
{
    public ListenerRender(BreakingESP module)
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

        double rangeSq =
            module.range.getValue() * module.range.getValue();
        for (Map.Entry<BlockPos, Float> entry : module.blocks.entrySet())
        {
            BlockPos pos = entry.getKey();
            float damage = entry.getValue();
            if (pos == null
                || mc.world.getBlockState(pos).getBlock() == Blocks.AIR
                || mc.player.getDistanceSq(pos) > rangeSq)
            {
                continue;
            }

            drawESP(pos, Math.max(0.0f, Math.min(1.0f, damage)));
        }

        for (Map.Entry<BlockPos, Integer> entry
                : module.animTicks.entrySet())
        {
            BlockPos pos = entry.getKey();
            int ticks = entry.getValue() + 1;
            if (mc.world.getBlockState(pos).getBlock() == Blocks.AIR
                || module.blocks.containsKey(pos)
                || mc.player.getDistanceSq(pos) > rangeSq)
            {
                module.animTicks.remove(pos);
                continue;
            }

            if (ticks > 140)
            {
                module.animTicks.remove(pos);
                continue;
            }

            module.animTicks.put(pos, ticks);
            drawESP(pos, Math.min(ticks, 140) / 140.0f);
        }
    }

    private void drawESP(BlockPos pos, float damage)
    {
        AxisAlignedBB bb = mc.world.getBlockState(pos)
            .getSelectedBoundingBox(mc.world, pos);
        double x = bb.minX + (bb.maxX - bb.minX) / 2.0;
        double y = bb.minY + (bb.maxY - bb.minY) / 2.0;
        double z = bb.minZ + (bb.maxZ - bb.minZ) / 2.0;
        double sizeX = damage * (bb.maxX - x);
        double sizeY = damage * (bb.maxY - y);
        double sizeZ = damage * (bb.maxZ - z);

        Color color = module.colorMode.getValue()
                == BreakingESP.ColorMode.PROGRESS
            ? new Color(damage <= 0.75f ? 200 : 0,
                        damage >= 0.751f ? 200 : 0,
                        0,
                        module.customColor.getValue().getAlpha())
            : module.customColor.getValue();

        AxisAlignedBB inBB = new AxisAlignedBB(
            x - (bb.maxX - x - sizeX),
            y - (bb.maxY - y - sizeY),
            z - (bb.maxZ - z - sizeZ),
            x + (bb.maxX - x - sizeX),
            y + (bb.maxY - y - sizeY),
            z + (bb.maxZ - z - sizeZ));
        AxisAlignedBB outBB = new AxisAlignedBB(
            x - sizeX, y - sizeY, z - sizeZ,
            x + sizeX, y + sizeY, z + sizeZ);
        AxisAlignedBB renderBB =
            module.mode.getValue() == BreakingESP.Mode.IN ? inBB : outBB;

        if (module.box.getValue())
        {
            RenderUtil.renderBox(renderBB,
                                 new Color(color.getRed(),
                                           color.getGreen(),
                                           color.getBlue(),
                                           76),
                                 color,
                                 module.lineWidth.getValue());
        }
        else if (module.line.getValue())
        {
            RenderUtil.renderBox(renderBB,
                                 new Color(0, 0, 0, 0),
                                 color,
                                 module.lineWidth.getValue());
        }
    }
}
