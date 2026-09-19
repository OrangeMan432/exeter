package me.earth.earthhack.impl.modules.movement.holesnap;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

final class ListenerTick extends ModuleListener<HoleSnap, TickEvent>
{
    public ListenerTick(HoleSnap module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.world == null
            || mc.player == null
            || mc.player.onGround
            || mc.player.motionY >= 0.0)
        {
            return;
        }

        BlockPos best = null;
        double bestDist = module.range.getValue() * module.range.getValue();
        int r = (int) Math.ceil(module.range.getValue());
        BlockPos feet = new BlockPos(mc.player.posX,
                                     mc.player.posY,
                                     mc.player.posZ);
        for (int x = -r; x <= r; x++)
        {
            for (int z = -r; z <= r; z++)
            {
                for (int y = -3; y <= 0; y++)
                {
                    BlockPos pos = feet.add(x, y, z);
                    if (!isHole(pos))
                    {
                        continue;
                    }

                    double dist = mc.player.getDistanceSq(
                        pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    if (dist < bestDist)
                    {
                        bestDist = dist;
                        best = pos;
                    }
                }
            }
        }

        if (best == null)
        {
            return;
        }

        double targetX = best.getX() + 0.5;
        double targetZ = best.getZ() + 0.5;
        mc.player.motionX =
            (targetX - mc.player.posX) * module.pull.getValue();
        mc.player.motionZ =
            (targetZ - mc.player.posZ) * module.pull.getValue();
        if (module.stepDown.getValue())
        {
            mc.player.motionY = Math.min(mc.player.motionY, -0.5);
        }
    }

    private boolean isHole(BlockPos pos)
    {
        if (mc.world.getBlockState(pos).getBlock() != Blocks.AIR
            || mc.world.getBlockState(pos.up()).getBlock() != Blocks.AIR
            || !isBlastProof(pos.down()))
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
}
