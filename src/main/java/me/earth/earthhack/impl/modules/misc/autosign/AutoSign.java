package me.earth.earthhack.impl.modules.misc.autosign;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.StringSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Auto-writes sign text with date support.
 * Ported from SalHack (AutoSign).
 */
public class AutoSign extends Module
{
    protected final Setting<String> line1 =
        register(new StringSetting("Line1", "Exeter"));
    protected final Setting<String> line2 =
        register(new StringSetting("Line2", "was"));
    protected final Setting<String> line3 =
        register(new StringSetting("Line3", "here"));
    protected final Setting<String> line4 =
        register(new StringSetting("Line4", "~date~"));
    protected final Setting<Boolean> autoComplete =
        register(new BooleanSetting("AutoComplete", true));

    boolean wasInSign;

    public AutoSign()
    {
        super("AutoSign", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Fills signs automatically, ~date~ supported."));
    }
}
