package me.earth.earthhack.impl.modules.misc.chatsuffix;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.StringSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Brands chat with Humza Client and optional fancy text.
 * Ported from Mio 0.6.9 (BetterChat suffix), Exeter flavored.
 */
public class ChatSuffix extends Module
{
    protected final Setting<String> suffix =
        register(new StringSetting("Suffix", " | Humza Client"));
    protected final Setting<Boolean> fancy =
        register(new BooleanSetting("Fancy", false));

    public ChatSuffix()
    {
        super("ChatSuffix", Category.Misc);
        this.listeners.add(new ListenerSend(this));
        this.setData(new SimpleData(this,
            "Signs your chat as Humza Client."));
    }
}
