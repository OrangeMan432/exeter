package me.earth.earthhack.impl.modules.render.smallshield;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerTick extends ModuleListener<SmallShield, TickEvent>
{
    public ListenerTick(SmallShield module)
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

        if (module.normalOffset.getValue())
        {
            mc.entityRenderer.itemRenderer.equippedProgressOffHand =
                module.offset.getValue();
        }
    }
}
