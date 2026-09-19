package me.earth.earthhack.impl.modules.misc.autoreply;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.StringSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Auto-replies to whispers.
 * Ported from GameSense (AutoReply).
 */
public class AutoReply extends Module
{
    protected final Setting<String> reply =
        register(new StringSetting("Reply", "I don't speak to newfags!"));

    public AutoReply()
    {
        super("AutoReply", Category.Misc);
        this.listeners.add(new ListenerChat(this));
        this.setData(new SimpleData(this,
            "Replies to /msg automatically."));
    }
}
