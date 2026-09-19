package me.earth.earthhack.impl.modules.combat.crystalaura;

import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.render.RenderUtil;

final class ListenerRender extends ModuleListener<CrystalAura, Render3DEvent>
{
    public ListenerRender(CrystalAura module)
    {
        super(module, Render3DEvent.class);
    }

    @Override
    public void invoke(Render3DEvent event)
    {
        if (module.render.getValue() && module.renderPos != null)
        {
            RenderUtil.renderBox(module.renderPos,
                                 module.renderColor.getValue(),
                                 1.0f);
        }
    }
}
