package me.earth.earthhack.impl.modules.misc.ghastfarmer;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;

final class ListenerTick extends ModuleListener<GhastFarmer, TickEvent>
{
    public ListenerTick(GhastFarmer module)
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

        Entity ghast = null;
        double ghastDist = Double.MAX_VALUE;
        for (Entity entity : mc.world.loadedEntityList)
        {
            if (!(entity instanceof EntityGhast))
            {
                continue;
            }

            double dist = mc.player.getDistance(entity);
            if (dist < ghastDist)
            {
                ghastDist = dist;
                ghast = entity;
            }
        }

        if (ghast != null)
        {
            if (module.notifySound.getValue())
            {
                mc.player.playSound(SoundEvents.BLOCK_NOTE_BELL, 1.0f, 1.0f);
            }

            mc.player.sendChatMessage("#goto "
                + (int) ghast.posX + " "
                + (int) ghast.posY + " "
                + (int) ghast.posZ);
            return;
        }

        for (Entity entity : mc.world.loadedEntityList)
        {
            if (entity instanceof EntityItem
                && ((EntityItem) entity).getItem().getItem()
                    == Items.GHAST_TEAR)
            {
                mc.player.sendChatMessage("#goto "
                    + (int) entity.posX + " "
                    + (int) entity.posY + " "
                    + (int) entity.posZ);
                return;
            }
        }

        mc.player.sendChatMessage("#goto "
            + module.homeX + " " + module.homeY + " " + module.homeZ);
    }
}
