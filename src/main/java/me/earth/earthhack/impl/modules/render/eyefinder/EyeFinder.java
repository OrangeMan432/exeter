package me.earth.earthhack.impl.modules.render.eyefinder;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Lines from heads to where entities look.
 * Ported from Kami Blue (EyeFinder).
 */
public class EyeFinder extends Module
{
    protected final Setting<Boolean> players =
        register(new BooleanSetting("Players", true));
    protected final Setting<Boolean> mobs =
        register(new BooleanSetting("Mobs", false));
    protected final Setting<Boolean> animals =
        register(new BooleanSetting("Animals", false));

    public EyeFinder()
    {
        super("EyeFinder", Category.Render);
        this.listeners.add(new ListenerRender(this));
        this.setData(new SimpleData(this,
            "See where everyone is looking."));
    }
}
