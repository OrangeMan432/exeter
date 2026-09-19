package me.earth.earthhack.impl.modules.combat.targetstrafe;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.entity.player.EntityPlayer;

final class ListenerTick extends ModuleListener<TargetStrafe, TickEvent>
{
    public ListenerTick(TargetStrafe module)
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

        EntityPlayer best = null;
        double bestDist =
            module.range.getValue() * module.range.getValue();
        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player == null
                || player == mc.player
                || player.isDead
                || Managers.FRIENDS.contains(player))
            {
                continue;
            }

            double dist = mc.player.getDistanceSq(player);
            if (dist < bestDist)
            {
                bestDist = dist;
                best = player;
            }
        }

        module.target = best;
    }
}
