package me.earth.earthhack.impl.modules.movement.antilevitate;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.init.MobEffects;

final class ListenerTick extends ModuleListener<AntiLevitate, TickEvent>
{
    public ListenerTick(AntiLevitate module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.player == null)
        {
            return;
        }

        if (mc.player.isPotionActive(MobEffects.LEVITATION))
        {
            mc.player.removeActivePotionEffect(MobEffects.LEVITATION);
        }
    }
}
