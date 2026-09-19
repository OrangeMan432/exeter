package me.earth.earthhack.impl.modules.combat.selfweb;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

final class ListenerTick extends ModuleListener<SelfWeb, TickEvent>
{
    public ListenerTick(SelfWeb module)
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

        if (mc.player.motionY > 0.0 && module.jumpDisable.getValue())
        {
            module.disable();
            return;
        }

        if (!module.lagTimer.passed(500) && module.noLag.getValue())
        {
            return;
        }

        if (!module.delayTimer.passed(module.delay.getValue()))
        {
            return;
        }

        int oldSlot = mc.player.inventory.currentItem;
        int webSlot = findWeb();
        if (webSlot == -1)
        {
            module.disable();
            return;
        }

        module.placements = 0;
        doSwap(webSlot);

        BlockPos feet = new BlockPos(mc.player.posX,
                                     mc.player.posY,
                                     mc.player.posZ);
        BlockPos down = feet.down();
        if (isReplaceable(down) && !hasWeb(down))
        {
            place(down);
        }

        if (module.smart.getValue())
        {
            for (int x = -1; x <= 1; x++)
            {
                for (int z = -1; z <= 1; z++)
                {
                    for (int y = -1; y <= 1; y++)
                    {
                        BlockPos pos = feet.add(x, y, z);
                        if (!isReplaceable(pos) || hasWeb(pos))
                        {
                            continue;
                        }

                        for (EntityPlayer enemy
                                : mc.world.playerEntities)
                        {
                            if (enemy == null
                                || enemy == mc.player
                                || enemy.isDead
                                || Managers.FRIENDS.contains(enemy))
                            {
                                continue;
                            }

                            if (enemy.getDistanceSq(pos) < 1.0
                                && module.placements
                                    < module.blocksPerTick.getValue())
                            {
                                place(pos);
                            }
                        }
                    }
                }
            }
        }

        if (module.above.getValue())
        {
            BlockPos head = feet.up(2);
            if (isReplaceable(head) && !hasWeb(head))
            {
                for (EntityPlayer enemy : mc.world.playerEntities)
                {
                    if (enemy != null
                        && enemy != mc.player
                        && !enemy.isDead
                        && !Managers.FRIENDS.contains(enemy)
                        && enemy.posY - mc.player.posY > 1.0)
                    {
                        place(head);
                        break;
                    }
                }
            }
        }

        doSwap(oldSlot);
        module.delayTimer.reset();
    }

    private void place(BlockPos pos)
    {
        if (module.placements >= module.blocksPerTick.getValue())
        {
            return;
        }

        mc.playerController.processRightClickBlock(
            mc.player,
            mc.world,
            pos,
            EnumFacing.UP,
            new Vec3d(pos).add(0.5, 0.5, 0.5),
            EnumHand.MAIN_HAND);
        mc.player.swingArm(EnumHand.MAIN_HAND);
        module.placements++;
    }

    private void doSwap(int slot)
    {
        if (module.swap.getValue() == SelfWeb.Swap.NORMAL)
        {
            mc.player.inventory.currentItem = slot;
        }
        else
        {
            mc.player.connection.sendPacket(
                new CPacketHeldItemChange(slot));
        }

        mc.playerController.updateController();
    }

    private int findWeb()
    {
        Item web = Item.getItemFromBlock(Blocks.WEB);
        for (int i = 0; i < 9; i++)
        {
            if (mc.player.inventory.getStackInSlot(i).getItem() == web)
            {
                return i;
            }
        }

        for (int i = 9; i < 36; i++)
        {
            if (mc.player.inventory.getStackInSlot(i).getItem() == web)
            {
                return i;
            }
        }

        return -1;
    }

    private boolean isReplaceable(BlockPos pos)
    {
        return mc.world.getBlockState(pos).getMaterial().isReplaceable();
    }

    private boolean hasWeb(BlockPos pos)
    {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.WEB;
    }
}
