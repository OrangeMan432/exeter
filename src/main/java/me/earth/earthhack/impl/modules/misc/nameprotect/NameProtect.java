package me.earth.earthhack.impl.modules.misc.nameprotect;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.StringSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Replaces your name in incoming chat, for streamers and alts.
 * Original Exeter implementation of Mio's NameProtect idea.
 */
public class NameProtect extends Module
{
    protected final Setting<String> name =
        register(new StringSetting("Name", "You"));

    public NameProtect()
    {
        super("NameProtect", Category.Misc);
        this.listeners.add(new ListenerChat(this));
        this.setData(new SimpleData(this,
            "Hides your name in chat."));
    }
}
