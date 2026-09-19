package me.earth.earthhack.impl.modules.misc.autoshulkerdupe;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;

final class ListenerTick extends ModuleListener<AutoShulkerDupe, TickEvent>
{
    public ListenerTick(AutoShulkerDupe module)
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

        switch (module.stage)
        {
            case 0:
                doDropStage();
                break;
            case 1:
                doCraftStage();
                break;
            case 2:
                doWaitStage();
                break;
            case 3:
                doBreakStage();
                break;
            default:
                break;
        }
    }

    private void doDropStage()
    {
        if (mc.player.inventory.getStackInSlot(module.slotShulk + 36)
                .isEmpty())
        {
            module.slotShulk = findFirstShulker();
            if (module.slotShulk == -1)
            {
                module.disable();
                return;
            }
        }

        mc.playerController.windowClick(
            0, module.slotShulk + 36, 0, ClickType.THROW, mc.player);
        if (mc.player.isSneaking())
        {
            mc.player.connection.sendPacket(new CPacketEntityAction(
                mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
        }

        if (module.workbenchPos != null)
        {
            mc.playerController.processRightClickBlock(
                mc.player,
                mc.world,
                module.workbenchPos,
                EnumFacing.UP,
                new Vec3d(module.workbenchPos),
                EnumHand.MAIN_HAND);
        }

        if (mc.player.isSneaking())
        {
            mc.player.connection.sendPacket(new CPacketEntityAction(
                mc.player, CPacketEntityAction.Action.START_SNEAKING));
        }

        module.stage = 1;
        module.tickPutItem = 0;
    }

    private void doCraftStage()
    {
        if (!(mc.currentScreen instanceof GuiCrafting))
        {
            return;
        }

        if (module.tickPutItem++ < module.itemCrafting.getValue())
        {
            return;
        }

        int woodSlot = module.slotWood < 9
            ? module.slotWood + 37
            : module.slotWood + 1;
        mc.playerController.windowClick(
            mc.player.openContainer.windowId,
            woodSlot,
            1,
            ClickType.PICKUP,
            mc.player);
        mc.playerController.windowClick(
            mc.player.openContainer.windowId, 1, 0, ClickType.PICKUP,
            mc.player);
        mc.playerController.updateController();
        module.stage = 2;
        module.tickPutItem = 0;
    }

    private void doWaitStage()
    {
        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty()
                && stack.getItem() instanceof ItemBlock
                && ((ItemBlock) stack.getItem()).getBlock()
                    instanceof BlockShulkerBox
                && stack.getCount() > 1)
            {
                mc.player.closeScreen();
                mc.player.inventory.currentItem = i;
                if (!mc.player.isSneaking())
                {
                    mc.player.connection.sendPacket(
                        new CPacketEntityAction(
                            mc.player,
                            CPacketEntityAction.Action.START_SNEAKING));
                }

                place(module.shulkerPos);
                if (!mc.player.isSneaking())
                {
                    mc.player.connection.sendPacket(
                        new CPacketEntityAction(
                            mc.player,
                            CPacketEntityAction.Action.STOP_SNEAKING));
                }

                module.stage = 3;
                module.tickPutItem = 0;
                module.beforePlaced = false;
                return;
            }
        }

        if (module.tickPutItem++ > module.maxStackWait.getValue())
        {
            module.stage = 0;
            mc.player.closeScreen();
            module.tickPutItem = 0;
        }
    }

    private void doBreakStage()
    {
        if (module.shulkerPos != null
            && getBlock(module.shulkerPos) instanceof BlockShulkerBox)
        {
            module.beforePlaced = true;
            mc.player.inventory.currentItem = module.slotPick;
            mc.player.swingArm(EnumHand.MAIN_HAND);
            mc.playerController.onPlayerDamageBlock(module.shulkerPos,
                                                    EnumFacing.UP);
        }
        else if (module.beforePlaced
            || module.tickPutItem++ > module.waitPlace.getValue())
        {
            module.stage = 0;
        }
    }

    private void place(BlockPos pos)
    {
        if (pos == null
            || !mc.world.getBlockState(pos).getMaterial().isReplaceable())
        {
            return;
        }

        for (EnumFacing side : EnumFacing.values())
        {
            BlockPos neighbour = pos.offset(side);
            if (mc.world.getBlockState(neighbour).getBlock()
                    .canCollideCheck(mc.world.getBlockState(neighbour),
                                     false)
                && !mc.world.getBlockState(neighbour).getMaterial()
                    .isReplaceable())
            {
                EnumFacing opposite = side.getOpposite();
                Vec3d hit = new Vec3d(neighbour).add(0.5, 0.5, 0.5).add(
                    new Vec3d(opposite.getDirectionVec()).scale(0.5));
                if (!mc.player.isSneaking())
                {
                    mc.player.connection.sendPacket(
                        new CPacketEntityAction(
                            mc.player,
                            CPacketEntityAction.Action.START_SNEAKING));
                }

                mc.playerController.processRightClickBlock(
                    mc.player, mc.world, neighbour, opposite, hit,
                    EnumHand.MAIN_HAND);
                mc.player.swingArm(EnumHand.MAIN_HAND);
                return;
            }
        }
    }

    private Block getBlock(BlockPos pos)
    {
        return pos == null
            ? null
            : mc.world.getBlockState(pos).getBlock();
    }

    int findFirstShulker()
    {
        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = mc.player.inventory.mainInventory.get(i);
            if (!stack.isEmpty()
                && stack.getItem() instanceof ItemBlock
                && ((ItemBlock) stack.getItem()).getBlock()
                    instanceof BlockShulkerBox)
            {
                return i;
            }
        }

        return -1;
    }
}
