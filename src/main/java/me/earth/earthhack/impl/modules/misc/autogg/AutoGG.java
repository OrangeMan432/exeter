package me.earth.earthhack.impl.modules.misc.autogg;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

import java.util.ArrayList;
import java.util.List;

/**
 * Says GG when enemies die.
 * Ported from Phobos 1.9 (AutoGG).
 */
public class AutoGG extends Module
{
    protected final Setting<Boolean> own =
        register(new BooleanSetting("Own", false));
    protected final Setting<Integer> delay =
        register(new NumberSetting<>("Delay", 5, 1, 20));

    static final List<String> MESSAGES = new ArrayList<>();

    static
    {
        MESSAGES.add("GG!");
        MESSAGES.add("gg no re");
        MESSAGES.add("Good fight!");
        MESSAGES.add("ez");
        MESSAGES.add("GG, better luck next time.");
    }

    public AutoGG()
    {
        super("AutoGG", Category.Misc);
        this.listeners.add(new ListenerDeath(this));
        this.setData(new SimpleData(this,
            "Celebrates your kills in chat."));
    }
}
