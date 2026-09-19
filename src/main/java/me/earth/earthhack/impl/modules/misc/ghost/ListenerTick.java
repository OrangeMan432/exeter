package me.earth.earthhack.impl.modules.misc.ghost;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerTick extends ModuleListener<Ghost, TickEvent>
{
    public ListenerTick(Ghost module)
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

        if (mc.player.getHealth() <= 0.0f)
        {
            mc.player.setHealth(20.0f);
            mc.player.isDead = false;
            module.ghosted = true;
            mc.displayGuiScreen(null);
            mc.player.setPositionAndUpdate(mc.player.posX,
                                           mc.player.posY,
                                           mc.player.posZ);
        }
    }
}
