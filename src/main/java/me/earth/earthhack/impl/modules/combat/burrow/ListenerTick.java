package me.earth.earthhack.impl.modules.combat.burrow;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

final class ListenerTick extends ModuleListener<Burrow, TickEvent>
{
    public ListenerTick(Burrow module)
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

        BlockPos feet = new BlockPos(mc.player.posX,
                                     mc.player.posY,
                                     mc.player.posZ);
        if (!mc.world.getBlockState(feet).getMaterial().isReplaceable())
        {
            if (module.autoDisable.getValue())
            {
                module.disable();
            }

            return;
        }

        int obby = findObsidian();
        if (obby == -1)
        {
            module.disable();
            return;
        }

        int oldSlot = mc.player.inventory.currentItem;
        mc.player.inventory.currentItem = obby;
        mc.playerController.updateController();
        mc.playerController.processRightClickBlock(
            mc.player,
            mc.world,
            feet,
            EnumFacing.UP,
            new Vec3d(feet).add(0.5, 0.5, 0.5),
            EnumHand.MAIN_HAND);
        mc.player.swingArm(EnumHand.MAIN_HAND);
        mc.player.inventory.currentItem = oldSlot;
        mc.playerController.updateController();

        for (int i = 0; i < module.risePackets.getValue(); i++)
        {
            mc.player.connection.sendPacket(new CPacketPlayer.Position(
                mc.player.posX,
                mc.player.posY + module.riseHeight.getValue(),
                mc.player.posZ,
                false));
        }

        if (module.autoDisable.getValue())
        {
            module.disable();
        }
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
