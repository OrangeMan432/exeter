package me.earth.earthhack.impl.modules.combat.anticityboss;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

final class ListenerTick extends ModuleListener<AntiCityBoss, TickEvent>
{
    public ListenerTick(AntiCityBoss module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        if (module.trapCheck.getValue() && !isTrapped())
        {
            return;
        }

        boolean enemyNear = false;
        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player != null
                && player != mc.player
                && !player.isDead
                && !Managers.FRIENDS.contains(player)
                && mc.player.getDistanceSq(player)
                    <= module.range.getValue() * module.range.getValue())
            {
                enemyNear = true;
                break;
            }
        }

        if (!enemyNear)
        {
            return;
        }

        int obby = findObsidian();
        if (obby == -1)
        {
            return;
        }

        BlockPos center = new BlockPos(mc.player.posX,
                                       mc.player.posY,
                                       mc.player.posZ);
        List<BlockPos> fill = new ArrayList<>();
        switch (mc.player.getHorizontalFacing())
        {
            case EAST:
                fill.add(center.east().east());
                fill.add(center.east().east().up());
                fill.add(center.east().east().east());
                fill.add(center.east().east().east().up());
                break;
            case WEST:
                fill.add(center.west().west());
                fill.add(center.west().west().up());
                fill.add(center.west().west().west());
                fill.add(center.west().west().west().up());
                break;
            case NORTH:
                fill.add(center.north().north());
                fill.add(center.north().north().up());
                fill.add(center.north().north().north());
                fill.add(center.north().north().north().up());
                break;
            case SOUTH:
                fill.add(center.south().south());
                fill.add(center.south().south().up());
                fill.add(center.south().south().south());
                fill.add(center.south().south().south().up());
                break;
            default:
                break;
        }

        int oldSlot = mc.player.inventory.currentItem;
        mc.player.inventory.currentItem = obby;
        mc.playerController.updateController();

        int placed = 0;
        for (BlockPos pos : fill)
        {
            if (placed >= module.blocksPerTick.getValue())
            {
                break;
            }

            if (!mc.world.getBlockState(pos).getMaterial().isReplaceable())
            {
                continue;
            }

            mc.playerController.processRightClickBlock(
                mc.player,
                mc.world,
                pos,
                EnumFacing.UP,
                new Vec3d(pos).add(0.5, 0.5, 0.5),
                EnumHand.MAIN_HAND);
            mc.player.swingArm(EnumHand.MAIN_HAND);
            placed++;
        }

        mc.player.inventory.currentItem = oldSlot;
        mc.playerController.updateController();
    }

    private boolean isTrapped()
    {
        BlockPos feet = new BlockPos(mc.player.posX,
                                     mc.player.posY,
                                     mc.player.posZ);
        for (EnumFacing facing : EnumFacing.HORIZONTALS)
        {
            if (mc.world.getBlockState(feet.offset(facing)).getMaterial()
                .isReplaceable())
            {
                return false;
            }
        }

        return !mc.world.getBlockState(feet.up(2)).getMaterial()
            .isReplaceable();
    }

    private int findObsidian()
    {
        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty()
                && stack.getItem() instanceof ItemBlock
                && ((ItemBlock) stack.getItem()).getBlock()
                    == Blocks.OBSIDIAN)
            {
                return i;
            }
        }

        return -1;
    }
}
