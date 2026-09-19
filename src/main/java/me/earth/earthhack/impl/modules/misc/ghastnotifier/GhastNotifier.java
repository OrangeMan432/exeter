package me.earth.earthhack.impl.modules.misc.ghastnotifier;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Calls out ghasts with coordinates and sound.
 * Ported from Mio 0.6.9 (GhastNotifier).
 */
public class GhastNotifier extends Module
{
    protected final Setting<Boolean> chat =
        register(new BooleanSetting("Chat", true));
    protected final Setting<Boolean> censorCoords =
        register(new BooleanSetting("CensorCoords", false));
    protected final Setting<Boolean> sound =
        register(new BooleanSetting("Sound", true));

    public GhastNotifier()
    {
        super("GhastNotifier", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Notifies you about ghasts for farming."));
    }

    @Override
    protected void onEnable()
    {
        ListenerTick.seen.clear();
    }
}
