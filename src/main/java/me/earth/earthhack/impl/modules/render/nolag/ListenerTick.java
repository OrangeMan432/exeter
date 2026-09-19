package me.earth.earthhack.impl.modules.render.nolag;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;

final class ListenerTick extends ModuleListener<NoLag, TickEvent>
{
    public ListenerTick(NoLag module)
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

        if (!module.glowing.getValue())
        {
            return;
        }

        for (Entity entity : mc.world.loadedEntityList)
        {
            if (entity.isGlowing())
            {
                entity.setGlowing(false);
            }
        }
    }
}
