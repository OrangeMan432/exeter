package me.earth.earthhack.impl.modules.misc.autogg;

import me.earth.earthhack.impl.event.events.misc.DeathEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Random;

final class ListenerDeath extends ModuleListener<AutoGG, DeathEvent>
{
    final Random random = new Random();

    public ListenerDeath(AutoGG module)
    {
        super(module, DeathEvent.class);
    }

    @Override
    public void invoke(DeathEvent event)
    {
        if (mc.player == null)
        {
            return;
        }

        EntityLivingBase entity = event.getEntity();
        if (!(entity instanceof EntityPlayer) || entity.isDead)
        {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;
        if (player == mc.player)
        {
            if (module.own.getValue())
            {
                mc.player.sendChatMessage("gg, i died.");
            }

            return;
        }

        if (Managers.FRIENDS.contains(player))
        {
            return;
        }

        mc.player.sendChatMessage(AutoGG.MESSAGES.get(
            random.nextInt(AutoGG.MESSAGES.size())));
    }
}
