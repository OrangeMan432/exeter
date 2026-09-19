package me.earth.earthhack.impl.modules.misc.pearlnotify;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;

final class ListenerTick extends ModuleListener<PearlNotify, TickEvent>
{
    public ListenerTick(PearlNotify module)
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

        Entity thrownPearl = null;
        for (Entity entity : mc.world.loadedEntityList)
        {
            if (entity instanceof EntityEnderPearl)
            {
                thrownPearl = entity;
                break;
            }
        }

        if (thrownPearl == null)
        {
            module.flag = true;
            return;
        }

        EntityPlayer closest = null;
        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (closest == null
                || closest.getDistance(thrownPearl)
                    > player.getDistance(thrownPearl))
            {
                closest = player;
            }
        }

        if (closest == mc.player)
        {
            module.flag = false;
        }

        if (closest != null && module.flag)
        {
            String heading = thrownPearl.getHorizontalFacing().toString();
            if (heading.equals("West"))
            {
                heading = "East";
            }
            else if (heading.equals("East"))
            {
                heading = "West";
            }

            boolean friend = Managers.FRIENDS.contains(closest);
            ChatUtil.sendMessage(
                (friend ? TextFormatting.AQUA : TextFormatting.RED)
                    + closest.getName()
                    + TextFormatting.GRAY
                    + " has just thrown a pearl heading "
                    + (friend ? TextFormatting.AQUA : TextFormatting.RED)
                    + heading + "!",
                closest.getEntityId());

            module.flag = false;
        }
    }
}
