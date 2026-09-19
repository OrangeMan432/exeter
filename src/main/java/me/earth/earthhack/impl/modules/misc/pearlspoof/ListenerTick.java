package me.earth.earthhack.impl.modules.misc.pearlspoof;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

final class ListenerTick extends ModuleListener<PearlSpoof, TickEvent>
{
    public ListenerTick(PearlSpoof module)
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

        int blockSlot = findBlock();
        int pearlSlot = findPearl();
        if (blockSlot == -1 || pearlSlot == -1)
        {
            module.disable();
            return;
        }

        BlockPos below =
            new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ);
        if (!mc.world.getBlockState(below).getMaterial().isReplaceable())
        {
            return;
        }

        int oldSlot = mc.player.inventory.currentItem;
        mc.player.inventory.currentItem = blockSlot;
        mc.playerController.updateController();
        if (module.packetPlace.getValue())
        {
            mc.player.connection.sendPacket(
                new CPacketHeldItemChange(blockSlot));
        }

        mc.playerController.processRightClickBlock(
            mc.player,
            mc.world,
            below,
            EnumFacing.UP,
            new Vec3d(below).add(0.5, 0.5, 0.5),
            EnumHand.MAIN_HAND);
        mc.player.swingArm(EnumHand.MAIN_HAND);

        mc.player.inventory.currentItem = pearlSlot;
        mc.playerController.updateController();
        mc.player.connection.sendPacket(
            new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));

        mc.player.inventory.currentItem = oldSlot;
        mc.playerController.updateController();
        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player != null
                && player != mc.player
                && !player.isDead
                && !Managers.FRIENDS.contains(player)
                && mc.player.getDistanceSq(player) < 4.0)
            {
                mc.player.motionX = 0.0;
                mc.player.motionY = 0.0;
                mc.player.motionZ = 0.0;
                break;
            }
        }

        module.disable();
    }

    private int findBlock()
    {
        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty()
                && stack.getItem() instanceof ItemBlock
                && ((ItemBlock) stack.getItem()).getBlock() != Blocks.AIR)
            {
                return i;
            }
        }

        return -1;
    }

    private int findPearl()
    {
        for (int i = 0; i < 9; i++)
        {
            if (mc.player.inventory.getStackInSlot(i).getItem()
                == Items.ENDER_PEARL)
            {
                return i;
            }
        }

        return -1;
    }
}
