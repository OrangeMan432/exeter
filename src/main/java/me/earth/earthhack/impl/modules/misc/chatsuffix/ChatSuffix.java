package me.earth.earthhack.impl.modules.misc.chatsuffix;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Appends a suffix to chat messages.
 * Ported from Mio 0.6.9 (BetterChat suffix).
 */
public class ChatSuffix extends Module
{
    protected final Setting<Boolean> suffix2b =
        register(new BooleanSetting("2b2tSuffix", false));

    public ChatSuffix()
    {
        super("ChatSuffix", Category.Misc);
        this.listeners.add(new ListenerSend(this));
        this.setData(new SimpleData(this,
            "Tags your chat messages."));
    }
}
