package me.earth.earthhack.impl.modules.player.flagdetect;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Notifies when the server lags you back.
 * Ported from Mio 0.6.9 (FlagDetect, notify part).
 */
public class FlagDetect extends Module
{
    protected final Setting<Boolean> chatNotify =
        register(new BooleanSetting("ChatNotify", true));

    public FlagDetect()
    {
        super("FlagDetect", Category.Player);
        this.listeners.add(new ListenerPosLook(this));
        this.setData(new SimpleData(this,
            "Detects and notifies server lagbacks."));
    }
}
