package me.earth.earthhack.impl.modules.render.storageesp;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

import java.awt.*;

/**
 * Boxes around storage blocks and containers.
 * Ported from Kami Blue (StorageESP).
 */
public class StorageESP extends Module
{
    protected final Setting<Double> range =
        register(new NumberSetting<>("Range", 64.0, 0.0, 256.0));
    protected final Setting<Color> chestColor =
        register(new ColorSetting("Chest", new Color(255, 128, 0, 255)));
    protected final Setting<Color> enderChestColor =
        register(new ColorSetting("EnderChest", new Color(180, 0, 255, 255)));
    protected final Setting<Color> shulkerColor =
        register(new ColorSetting("Shulker", new Color(255, 255, 0, 255)));
    protected final Setting<Color> furnaceColor =
        register(new ColorSetting("Furnace", new Color(128, 128, 128, 255)));
    protected final Setting<Color> hopperColor =
        register(new ColorSetting("Hopper", new Color(180, 0, 0, 255)));
    protected final Setting<Color> dispenserColor =
        register(new ColorSetting("Dispenser", new Color(0, 128, 255, 255)));
    protected final Setting<Color> minecartColor =
        register(new ColorSetting("Minecart", new Color(255, 128, 0, 255)));
    protected final Setting<Color> frameColor =
        register(new ColorSetting("ItemFrame", new Color(255, 255, 0, 255)));
    protected final Setting<Boolean> minecarts =
        register(new BooleanSetting("Minecarts", true));
    protected final Setting<Boolean> frames =
        register(new BooleanSetting("ItemFrames", true));
    protected final Setting<Float> height =
        register(new NumberSetting<>("Height", 1.0f, 0.0f, 1.0f));

    public StorageESP()
    {
        super("StorageESP", Category.Render);
        this.listeners.add(new ListenerRender(this));
        this.setData(new SimpleData(this,
            "Draws boxes around chests, shulkers and containers."));
    }
}
