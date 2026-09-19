package me.earth.earthhack.impl.modules.misc.automount;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityMule;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.util.EnumHand;

final class ListenerTick extends ModuleListener<AutoMount, TickEvent>
{
    public ListenerTick(AutoMount module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.world == null
            || mc.player == null
            || mc.player.isRiding()
            || !module.timer.passed((long) (module.delay.getValue() * 1000)))
        {
            return;
        }

        module.timer.reset();
        Entity best = null;
        double bestDist =
            module.range.getValue() * module.range.getValue();
        for (Entity entity : mc.world.loadedEntityList)
        {
            if (!isMountable(entity))
            {
                continue;
            }

            double dist = mc.player.getDistanceSq(entity);
            if (dist < bestDist)
            {
                bestDist = dist;
                best = entity;
            }
        }

        if (best != null)
        {
            mc.playerController.interactWithEntity(
                mc.player, best, EnumHand.MAIN_HAND);
        }
    }

    private boolean isMountable(Entity entity)
    {
        if (module.boats.getValue() && entity instanceof EntityBoat)
        {
            return true;
        }

        if (module.horses.getValue()
            && (entity instanceof EntityHorse
                || entity instanceof EntitySkeletonHorse))
        {
            return true;
        }

        if (module.donkeys.getValue()
            && (entity instanceof EntityDonkey
                || entity instanceof EntityMule))
        {
            return true;
        }

        if (module.pigs.getValue() && entity instanceof EntityPig)
        {
            return true;
        }

        return module.llamas.getValue() && entity instanceof EntityLlama;
    }
}
