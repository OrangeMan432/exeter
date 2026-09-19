package me.earth.earthhack.impl.modules.misc.popnotify;

import me.earth.earthhack.impl.event.events.misc.DeathEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;

final class ListenerDeath extends ModuleListener<PopNotify, DeathEvent>
{
    public ListenerDeath(PopNotify module)
    {
        super(module, DeathEvent.class);
    }

    @Override
    public void invoke(DeathEvent event)
    {
        EntityLivingBase entity = event.getEntity();
        if (!(entity instanceof EntityPlayer))
        {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;
        Integer pops = module.totemPops.remove(player.getName());
        if (pops == null)
        {
            return;
        }

        boolean friend = Managers.FRIENDS.contains(player);
        ChatUtil.sendMessage(
            TextFormatting.RESET
                + (friend ? TextFormatting.AQUA : TextFormatting.WHITE)
                + player.getName()
                + TextFormatting.RESET + " died after popping "
                + (friend ? TextFormatting.AQUA : TextFormatting.WHITE)
                + TextFormatting.BOLD + pops + TextFormatting.RESET
                + (pops == 1 ? " totem." : " totems."),
            -123456789);
    }
}
