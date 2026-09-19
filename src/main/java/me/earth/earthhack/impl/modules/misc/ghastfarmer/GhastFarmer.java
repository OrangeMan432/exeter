package me.earth.earthhack.impl.modules.misc.ghastfarmer;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Hunts ghasts and tears through Baritone goto.
 * Ported from Mio 0.6.9 (GhastFarmer). Needs Baritone installed.
 */
public class GhastFarmer extends Module
{
    protected final Setting<Boolean> notifySound =
        register(new BooleanSetting("Sound", false));

    int homeX;
    int homeY;
    int homeZ;

    public GhastFarmer()
    {
        super("GhastFarmer", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Farms ghasts via Baritone goto. Needs Baritone."));
    }

    @Override
    protected void onEnable()
    {
        if (mc.player == null || mc.world == null)
        {
            disable();
            return;
        }

        homeX = (int) mc.player.posX;
        homeY = (int) mc.player.posY;
        homeZ = (int) mc.player.posZ;
    }

    @Override
    protected void onDisable()
    {
        if (mc.player != null)
        {
            mc.player.sendChatMessage("#stop");
        }
    }
}
