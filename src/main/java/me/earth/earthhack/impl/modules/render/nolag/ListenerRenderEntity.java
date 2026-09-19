package me.earth.earthhack.impl.modules.render.nolag;

import me.earth.earthhack.impl.event.events.render.RenderEntityEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.projectile.EntityWitherSkull;

final class ListenerRenderEntity extends
        ModuleListener<NoLag, RenderEntityEvent>
{
    public ListenerRenderEntity(NoLag module)
    {
        super(module, RenderEntityEvent.class);
    }

    @Override
    public void invoke(RenderEntityEvent event)
    {
        if (event.getEntity() == null)
        {
            return;
        }

        if (module.skulls.getValue()
            && event.getEntity() instanceof EntityWitherSkull)
        {
            event.setCancelled(true);
        }

        if (module.tnt.getValue()
            && event.getEntity() instanceof EntityTNTPrimed)
        {
            event.setCancelled(true);
        }

        if (module.parrots.getValue()
            && event.getEntity() instanceof EntityParrot)
        {
            event.setCancelled(true);
        }
    }
}
