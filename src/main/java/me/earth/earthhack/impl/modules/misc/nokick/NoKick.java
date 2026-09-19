package me.earth.earthhack.impl.modules.misc.nokick;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Removes common crash and kick vectors.
 * Ported from GameSense (NoKick, no-mixin parts).
 */
public class NoKick extends Module
{
    protected final Setting<Boolean> noSlimeCrash =
        register(new BooleanSetting("Slime", true));
    protected final Setting<Boolean> noOffhandCrash =
        register(new BooleanSetting("Offhand", false));

    public NoKick()
    {
        super("NoKick", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerSound(this));
        this.setData(new SimpleData(this,
            "Deletes oversized slimes and bad equip sounds."));
    }
}
