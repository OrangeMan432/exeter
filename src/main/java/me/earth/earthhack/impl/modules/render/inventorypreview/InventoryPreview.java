package me.earth.earthhack.impl.modules.render.inventorypreview;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

import java.awt.*;

/**
 * Shows your inventory on screen without opening it.
 * Ported from Mio 0.6.9 (InventoryPreview).
 */
public class InventoryPreview extends Module
{
    protected final Setting<Integer> x =
        register(new NumberSetting<>("X", 500, 0, 2000));
    protected final Setting<Integer> y =
        register(new NumberSetting<>("Y", 2, 0, 1200));
    protected final Setting<Color> lineColor =
        register(new ColorSetting("LineColor", new Color(10, 10, 10, 100)));
    protected final Setting<Color> rectColor =
        register(new ColorSetting("RectColor", new Color(10, 10, 10, 50)));

    public InventoryPreview()
    {
        super("InventoryPreview", Category.Render);
        this.listeners.add(new ListenerRender2D(this));
        this.setData(new SimpleData(this,
            "See your inventory without opening it."));
    }
}
