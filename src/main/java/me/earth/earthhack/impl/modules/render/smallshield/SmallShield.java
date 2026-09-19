package me.earth.earthhack.impl.modules.render.smallshield;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Lowers your offhand like the pros.
 * Ported from Phobos 1.9 (SmallShield).
 */
public class SmallShield extends Module
{
    protected final Setting<Boolean> normalOffset =
        register(new BooleanSetting("OffNormal", false));
    protected final Setting<Float> offset =
        register(new NumberSetting<>("Offset", 0.7f, 0.0f, 1.0f));
    protected final Setting<Float> offX =
        register(new NumberSetting<>("OffX", 0.0f, -1.0f, 1.0f));
    protected final Setting<Float> offY =
        register(new NumberSetting<>("OffY", 0.0f, -1.0f, 1.0f));
    protected final Setting<Float> mainX =
        register(new NumberSetting<>("MainX", 0.0f, -1.0f, 1.0f));
    protected final Setting<Float> mainY =
        register(new NumberSetting<>("MainY", 0.0f, -1.0f, 1.0f));

    public SmallShield()
    {
        super("SmallShield", Category.Render);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Lowers the offhand item."));
    }
}
