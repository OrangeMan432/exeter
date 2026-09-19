package me.earth.earthhack.impl.modules.render.portal;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws boxes around nether portals for travel.
 * Ported from Phobos 1.9 (PortalESP).
 */
public class PortalESP extends Module
{
    protected final Setting<Integer> distance =
        register(new NumberSetting<>("Distance", 60, 10, 100));
    protected final Setting<Color> color =
        register(new ColorSetting("Color", new Color(204, 0, 153, 255)));
    protected final Setting<Boolean> box =
        register(new BooleanSetting("Box", false));
    protected final Setting<Integer> boxAlpha =
        register(new NumberSetting<>("BoxAlpha", 125, 0, 255));
    protected final Setting<Boolean> outline =
        register(new BooleanSetting("Outline", true));
    protected final Setting<Float> lineWidth =
        register(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 5.0f));

    final List<BlockPos> portals = new ArrayList<>();
    int cooldown;

    public PortalESP()
    {
        super("PortalESP", Category.Render);
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerRender(this));
        this.setData(new SimpleData(this,
            "Highlights nether portal blocks."));
    }
}
