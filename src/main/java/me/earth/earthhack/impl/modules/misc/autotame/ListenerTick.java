package me.earth.earthhack.impl.modules.misc.autotame;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.text.ChatUtil;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;

final class ListenerTick extends ModuleListener<AutoTame, TickEvent>
{
    public ListenerTick(AutoTame module)
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

        if (module.target != null)
        {
            if (module.target.isDead || module.target.isTame())
            {
                ChatUtil.sendMessage(TextFormatting.GREEN
                    + "[AutoTame] Tamed "
                    + module.target.getName() + ", disabling.");
                module.target = null;
                module.disable();
                return;
            }

            if (!mc.player.isRiding()
                && module.timer.passed(
                    (long) (module.delay.getValue() * 1000)))
            {
                module.timer.reset();
                mc.playerController.interactWithEntity(
                    mc.player, module.target, EnumHand.MAIN_HAND);
            }

            return;
        }

        AbstractHorse best = null;
        double bestDist =
            module.range.getValue() * module.range.getValue();
        for (Object object : mc.world.loadedEntityList)
        {
            if (!(object instanceof AbstractHorse))
            {
                continue;
            }

            AbstractHorse horse = (AbstractHorse) object;
            if (horse.isTame() || horse.isDead)
            {
                continue;
            }

            double dist = mc.player.getDistanceSq(horse);
            if (dist < bestDist)
            {
                bestDist = dist;
                best = horse;
            }
        }

        if (best != null)
        {
            module.target = best;
            ChatUtil.sendMessage(TextFormatting.GOLD
                + "[AutoTame] Taming " + best.getName() + ".");
        }
    }
}
