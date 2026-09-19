package me.earth.earthhack.impl.modules.movement.holetp;

import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

final class ListenerMotion extends ModuleListener<HoleTP, MotionUpdateEvent>
{
    public ListenerMotion(HoleTP module)
    {
        super(module, MotionUpdateEvent.class);
    }

    @Override
    public void invoke(MotionUpdateEvent event)
    {
        if (mc.player == null
            || mc.world == null
            || module.shouldReturn())
        {
            return;
        }

        switch (event.getStage())
        {
            case POST:
                break;
            default:
                return;
        }

        if (!mc.player.onGround)
        {
            if (mc.gameSettings.keyBindJump.isKeyDown())
            {
                module.jumped = true;
            }
        }
        else
        {
            module.jumped = false;
        }

        if (!module.jumped
            && mc.player.fallDistance < 0.5f
            && isInHole()
            && mc.player.posY - nearestBlockBelow() <= 1.125
            && mc.player.posY - nearestBlockBelow() <= 0.95
            && !isOnLiquid()
            && !isInLiquid())
        {
            if (!mc.player.onGround)
            {
                module.packets++;
            }

            if (!mc.player.onGround
                && !mc.player.isInsideOfMaterial(Material.WATER)
                && !mc.player.isInsideOfMaterial(Material.LAVA)
                && !mc.gameSettings.keyBindJump.isKeyDown()
                && !mc.player.isOnLadder()
                && module.packets > 0)
            {
                BlockPos pos = new BlockPos(mc.player.posX,
                                            mc.player.posY,
                                            mc.player.posZ);
                for (double offset : module.oneBlockPositions)
                {
                    mc.player.connection.sendPacket(
                        new CPacketPlayer.Position(pos.getX() + 0.5,
                                                   mc.player.posY - offset,
                                                   pos.getZ() + 0.5,
                                                   true));
                }

                mc.player.setPosition(pos.getX() + 0.5,
                                      nearestBlockBelow() + 0.1,
                                      pos.getZ() + 0.5);
                module.packets = 0;
            }
        }
    }

    private boolean isInHole()
    {
        BlockPos pos = new BlockPos(mc.player.posX,
                                    mc.player.posY,
                                    mc.player.posZ);
        if (mc.world.getBlockState(pos).getBlock() != Blocks.AIR
            && mc.world.getBlockState(pos).getBlock() != Blocks.WATER)
        {
            return false;
        }

        for (EnumFacing facing : EnumFacing.HORIZONTALS)
        {
            if (mc.world.getBlockState(pos.offset(facing)).getBlock()
                    != Blocks.OBSIDIAN
                && mc.world.getBlockState(pos.offset(facing)).getBlock()
                    != Blocks.BEDROCK)
            {
                return false;
            }
        }

        return true;
    }

    private double nearestBlockBelow()
    {
        BlockPos feet = new BlockPos(mc.player.posX,
                                     mc.player.posY,
                                     mc.player.posZ);
        for (int y = feet.getY(); y > 0; y--)
        {
            BlockPos pos = new BlockPos(feet.getX(), y, feet.getZ());
            if (mc.world.getBlockState(pos).getMaterial().isSolid())
            {
                return y;
            }
        }

        return -1.0;
    }

    private boolean isOnLiquid()
    {
        BlockPos pos = new BlockPos(mc.player.posX,
                                    mc.player.posY - 0.5,
                                    mc.player.posZ);
        return mc.world.getBlockState(pos).getBlock() == Blocks.WATER
            || mc.world.getBlockState(pos).getBlock() == Blocks.LAVA;
    }

    private boolean isInLiquid()
    {
        return mc.player.isInsideOfMaterial(Material.WATER)
            || mc.player.isInsideOfMaterial(Material.LAVA);
    }
}
