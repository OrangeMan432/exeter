package me.earth.earthhack.impl.modules.combat.autocity;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

final class ListenerTick extends ModuleListener<AutoCity, TickEvent>
{
    public ListenerTick(AutoCity module)
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

        if (module.current != null
            && mc.world.getBlockState(module.current).getBlock()
                == Blocks.AIR)
        {
            module.current = null;
        }

        if (module.current == null)
        {
            module.current = findCityBlock();
            if (module.current == null)
            {
                if (module.autoDisable.getValue())
                {
                    ChatUtil.sendMessage(TextFormatting.RED
                        + "[AutoCity] Nothing to city, disabling.");
                    module.disable();
                }

                return;
            }

            mc.playerController.clickBlock(module.current,
                                           EnumFacing.UP);
        }

        int pickSlot = findPickaxe();
        if (pickSlot == -1)
        {
            ChatUtil.sendMessage(TextFormatting.RED
                + "[AutoCity] No diamond pickaxe, disabling.");
            module.disable();
            return;
        }

        if (module.autoSwitch.getValue())
        {
            if (module.lastSlot == -1)
            {
                module.lastSlot = mc.player.inventory.currentItem;
            }

            mc.player.inventory.currentItem = pickSlot;
            mc.playerController.updateController();
        }

        mc.playerController.onPlayerDamageBlock(module.current,
                                                EnumFacing.UP);
        mc.player.swingArm(EnumHand.MAIN_HAND);
    }

    private BlockPos findCityBlock()
    {
        BlockPos best = null;
        double bestDist = module.range.getValue() * module.range.getValue();
        for (EntityPlayer enemy : mc.world.playerEntities)
        {
            if (enemy == null
                || enemy == mc.player
                || enemy.isDead
                || Managers.FRIENDS.contains(enemy)
                || mc.player.getDistanceSq(enemy) > bestDist)
            {
                continue;
            }

            BlockPos hole = new BlockPos(enemy.posX,
                                         enemy.posY,
                                         enemy.posZ);
            if (!isHole(hole))
            {
                continue;
            }

            for (EnumFacing facing : EnumFacing.HORIZONTALS)
            {
                BlockPos city = hole.offset(facing);
                if (mc.world.getBlockState(city).getBlock()
                        != Blocks.OBSIDIAN)
                {
                    continue;
                }

                double dist = mc.player.getDistanceSq(city);
                if (dist < bestDist)
                {
                    bestDist = dist;
                    best = city;
                }
            }
        }

        return best;
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

    private int findPickaxe()
    {
        for (int i = 0; i < 9; i++)
        {
            if (mc.player.inventory.getStackInSlot(i).getItem()
                == Items.DIAMOND_PICKAXE)
            {
                return i;
            }
        }

        return -1;
    }
}
