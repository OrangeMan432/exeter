package me.earth.earthhack.impl.modules.misc.globallocation;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Logs world events (thunder, wither, dragon) with positions.
 * Ported from SalHack (GlobalLocation).
 */
public class GlobalLocation extends Module
{
    protected final Setting<Boolean> thunder =
        register(new BooleanSetting("Thunder", true));
    protected final Setting<Boolean> wither =
        register(new BooleanSetting("Wither", false));
    protected final Setting<Boolean> dragon =
        register(new BooleanSetting("EnderDragon", false));
    protected final Setting<Boolean> endPortal =
        register(new BooleanSetting("EndPortal", false));

    public GlobalLocation()
    {
        super("GlobalLocation", Category.Misc);
        this.listeners.add(new ListenerSound(this));
        this.listeners.add(new ListenerEffect(this));
        this.setData(new SimpleData(this,
            "Logs thunder strikes and boss spawns."));
    }
}
