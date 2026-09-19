package me.earth.earthhack.impl.modules.player.echestbp;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Keeps ender chest access after closing it.
 * Ported from Phobos 1.9 (EchestBP).
 */
public class EchestBP extends Module
{
    Object screen;

    public EchestBP()
    {
        super("EchestBP", Category.Player);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Reopens your ender chest after closing."));
    }

    @Override
    protected void onDisable()
    {
        if (mc.player != null && screen != null)
        {
            mc.displayGuiScreen((net.minecraft.client.gui.GuiScreen) screen);
        }

        screen = null;
    }
}
