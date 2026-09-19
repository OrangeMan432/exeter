package me.earth.earthhack.impl.modules.render.combatinfo;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.entity.player.EntityPlayer;

final class ListenerRender2D
        extends ModuleListener<CombatInfo, Render2DEvent>
{
    public ListenerRender2D(CombatInfo module)
    {
        super(module, Render2DEvent.class);
    }

    @Override
    public void invoke(Render2DEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        EntityPlayer best = null;
        double bestDist = module.range.getValue();
        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player == null
                || player == mc.player
                || player.isDead
                || Managers.FRIENDS.contains(player))
            {
                continue;
            }

            double dist = mc.player.getDistance(player);
            if (dist < bestDist)
            {
                bestDist = dist;
                best = player;
            }
        }

        if (best == null)
        {
            return;
        }

        String text = best.getName()
            + " HP:" + (int) (best.getHealth() + best.getAbsorptionAmount())
            + " " + (int) bestDist + "m";
        mc.fontRenderer.drawStringWithShadow(
            text, module.x.getValue(), module.y.getValue(), 0xFFFFFF);
    }
}
