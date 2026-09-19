package me.earth.earthhack.impl.modules.render.textradar;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Lists nearby players with distance and height.
 * Ported from GameSense (TextRadar).
 */
public class TextRadar extends Module
{
    protected final Setting<Display> display =
        register(new EnumSetting<>("Display", Display.All));
    protected final Setting<Boolean> sortUp =
        register(new BooleanSetting("SortUp", false));
    protected final Setting<Boolean> sortRight =
        register(new BooleanSetting("SortRight", false));
    protected final Setting<Integer> range =
        register(new NumberSetting<>("Range", 100, 1, 260));
    protected final Setting<Integer> x =
        register(new NumberSetting<>("X", 0, 0, 2000));
    protected final Setting<Integer> y =
        register(new NumberSetting<>("Y", 50, 0, 1200));

    public TextRadar()
    {
        super("TextRadar", Category.Render);
        this.listeners.add(new ListenerRender2D(this));
        this.setData(new SimpleData(this,
            "Text list of players around you."));
    }

    public enum Display
    {
        All,
        Friend,
        Enemy
    }
}
