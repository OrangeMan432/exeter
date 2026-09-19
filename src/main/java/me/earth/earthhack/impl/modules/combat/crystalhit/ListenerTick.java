package me.earth.earthhack.impl.modules.combat.crystalhit;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumHand;

final class ListenerTick extends ModuleListener<CrystalHit, TickEvent>
{
    public ListenerTick(CrystalHit module)
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

        EntityEnderCrystal best = null;
        double bestDist = module.range.getValue();
        for (Entity entity : mc.world.loadedEntityList)
        {
            if (!(entity instanceof EntityEnderCrystal) || entity.isDead)
            {
                continue;
            }

            double dist = mc.player.getDistance(entity);
            if (dist < bestDist)
            {
                bestDist = dist;
                best = (EntityEnderCrystal) entity;
            }
        }

        if (best == null || module.delayTime++ < module.delay.getValue())
        {
            return;
        }

        module.delayTime = 0;
        int oldSlot = mc.player.inventory.currentItem;
        if (module.antiWeakness.getValue()
            && mc.player.isPotionActive(MobEffects.WEAKNESS))
        {
            int sword = findSword();
            if (sword != -1)
            {
                mc.player.inventory.currentItem = sword;
                mc.playerController.updateController();
            }
        }

        if (module.packetBreak.getValue())
        {
            mc.player.connection.sendPacket(
                new CPacketUseEntity(best));
        }
        else
        {
            mc.playerController.attackEntity(mc.player, best);
        }

        if (module.swing.getValue())
        {
            mc.player.swingArm(EnumHand.MAIN_HAND);
        }

        if (oldSlot != mc.player.inventory.currentItem)
        {
            mc.player.connection.sendPacket(
                new CPacketHeldItemChange(oldSlot));
            mc.player.inventory.currentItem = oldSlot;
            mc.playerController.updateController();
        }
    }

    private int findSword()
    {
        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemSword
                && stack.getItem() != Items.DIAMOND_SWORD)
            {
                return i;
            }

            if (!stack.isEmpty()
                && stack.getItem() == Items.DIAMOND_SWORD)
            {
                return i;
            }
        }

        return -1;
    }
}
