package me.earth.earthhack.impl.modules.combat.anticrystal;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.minecraft.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

final class ListenerTick extends ModuleListener<AntiCrystal, TickEvent>
{
    public ListenerTick(AntiCrystal module)
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

        if (module.delayTicks++ < module.tickDelay.getValue())
        {
            return;
        }

        module.delayTicks = 0;
        if (module.onlyIfEnemy.getValue() && !enemyNear())
        {
            return;
        }

        int placed = 0;
        for (Entity entity : mc.world.loadedEntityList)
        {
            if (!(entity instanceof EntityEnderCrystal)
                || mc.player.getDistance(entity)
                    > module.rangePlace.getValue())
            {
                continue;
            }

            if (module.damageCheck.getValue())
            {
                float damage = DamageUtil.calculate(
                    entity.posX, entity.posY, entity.posZ, mc.player)
                    * module.biasDamage.getValue().floatValue();
                if (damage < module.damageMin.getValue()
                    && damage < mc.player.getHealth())
                {
                    continue;
                }
            }

            BlockPos pos = new BlockPos(
                entity.posX, entity.posY, entity.posZ);
            if (mc.world.getBlockState(pos).getBlock()
                    != Blocks.AIR)
            {
                continue;
            }

            int slot = findMaterial();
            if (slot == -1)
            {
                return;
            }

            place(pos, slot);
            if (++placed >= module.blocksPerTick.getValue())
            {
                return;
            }

            if (module.sneaking)
            {
                mc.player.connection.sendPacket(new CPacketEntityAction(
                    mc.player,
                    CPacketEntityAction.Action.STOP_SNEAKING));
                module.sneaking = false;
            }
        }
    }

    private boolean enemyNear()
    {
        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player != null
                && player != mc.player
                && !player.isDead
                && !Managers.FRIENDS.contains(player)
                && mc.player.getDistance(player)
                    <= module.enemyRange.getValue())
            {
                return true;
            }
        }

        return false;
    }

    private int findMaterial()
    {
        Item want = module.blockPlace.getValue()
                == AntiCrystal.BlockPlace.Pressure
            ? Item.getItemFromBlock(Blocks.WOODEN_PRESSURE_PLATE)
            : Items.STRING;
        for (int i = 0; i < 9; i++)
        {
            if (mc.player.inventory.getStackInSlot(i).getItem() == want)
            {
                return i;
            }
        }

        return -1;
    }

    private void place(BlockPos pos, int slot)
    {
        int oldSlot = mc.player.inventory.currentItem;
        mc.player.inventory.currentItem = slot;
        mc.playerController.updateController();

        BlockPos below = pos.down();
        mc.playerController.processRightClickBlock(
            mc.player,
            mc.world,
            below,
            EnumFacing.UP,
            new Vec3d(below).add(0.5, 1.0, 0.5),
            EnumHand.MAIN_HAND);
        mc.player.swingArm(EnumHand.MAIN_HAND);

        if (module.switchBack.getValue())
        {
            mc.player.inventory.currentItem = oldSlot;
            mc.playerController.updateController();
        }
    }
}
