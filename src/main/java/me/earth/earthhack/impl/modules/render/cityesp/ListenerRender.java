package me.earth.earthhack.impl.modules.render.cityesp;

import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

final class ListenerRender extends ModuleListener<CityESP, Render3DEvent>
{
    public ListenerRender(CityESP module)
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

        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player == null
                || player.isDead
                || player == mc.player && !module.self.getValue()
                || player != mc.player
                    && Managers.FRIENDS.contains(player)
                || mc.player.getDistance(player)
                    > module.range.getValue())
            {
                continue;
            }

            BlockPos hole = new BlockPos(player.posX,
                                         player.posY,
                                         player.posZ);
            if (!isHole(hole))
            {
                continue;
            }

            for (EnumFacing facing : EnumFacing.HORIZONTALS)
            {
                BlockPos city = hole.offset(facing);
                if (mc.world.getBlockState(city).getBlock()
                        == Blocks.OBSIDIAN)
                {
                    RenderUtil.renderBox(city,
                                         module.cityColor.getValue(),
                                         module.height.getValue());
                }
            }
        }
    }

    private boolean isHole(BlockPos pos)
    {
        if (!isAir(pos) || !isAir(pos.up()))
        {
            return false;
        }

        if (!isBlastProof(pos.down()))
        {
            return false;
        }

        for (EnumFacing facing : EnumFacing.HORIZONTALS)
        {
            if (!isBlastProof(pos.offset(facing)))
            {
                return false;
            }
        }

        return true;
    }

    private boolean isBlastProof(BlockPos pos)
    {
        return mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN
            || mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
    }

    private boolean isAir(BlockPos pos)
    {
        return mc.world.getBlockState(pos).getBlock() == Blocks.AIR;
    }
}
