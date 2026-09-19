package me.earth.earthhack.impl.modules.render.shulkerpreview;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Draws shulker contents inside tooltips.
 * Original Exeter addition for Humza Client.
 */
public class ShulkerPreview extends Module
{
    protected final Setting<Boolean> showName =
        register(new BooleanSetting("ShowName", true));

    public ShulkerPreview()
    {
        super("ShulkerPreview", Category.Render);
        this.listeners.add(new ListenerToolTip(this));
        this.setData(new SimpleData(this,
            "See inside shulkers from tooltips."));
    }
}
