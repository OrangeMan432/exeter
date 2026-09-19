package me.earth.earthhack.impl.modules.misc.nokick;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntitySlime;

final class ListenerTick extends ModuleListener<NoKick, TickEvent>
{
    public ListenerTick(NoKick module)
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

        if (!module.noSlimeCrash.getValue())
        {
            return;
        }

        for (Entity entity : mc.world.loadedEntityList)
        {
            if (entity instanceof EntitySlime
                && ((EntitySlime) entity).getSlimeSize() > 4)
            {
                mc.world.removeEntity(entity);
            }
        }
    }
}
