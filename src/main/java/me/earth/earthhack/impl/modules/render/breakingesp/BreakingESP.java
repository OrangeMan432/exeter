package me.earth.earthhack.impl.modules.render.breakingesp;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Highlights blocks being broken around you, with progress.
 * Ported from Mio 0.6.9 (BreakingESP), adapted to break-anim packets.
 */
public class BreakingESP extends Module
{
    protected final Setting<Boolean> showSelf =
        register(new BooleanSetting("ShowSelf", true));
    protected final Setting<Mode> mode =
        register(new EnumSetting<>("Mode", Mode.OUT));
    protected final Setting<Boolean> box =
        register(new BooleanSetting("Box", true));
    protected final Setting<Boolean> line =
        register(new BooleanSetting("Line", true));
    protected final Setting<Float> lineWidth =
        register(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 3.0f));
    protected final Setting<Double> range =
        register(new NumberSetting<>("Range", 20.0, 1.0, 50.0));
    protected final Setting<ColorMode> colorMode =
        register(new EnumSetting<>("ColorMode", ColorMode.PROGRESS));
    protected final Setting<Color> customColor =
        register(new ColorSetting("Color", new Color(125, 125, 213, 136)));

    final Map<BlockPos, Float> blocks = new ConcurrentHashMap<>();
    final Map<BlockPos, Integer> animTicks = new ConcurrentHashMap<>();

    public BreakingESP()
    {
        super("BreakingESP", Category.Render);
        this.listeners.add(new ListenerRender(this));
        this.listeners.add(new ListenerBreakAnim(this));
        this.listeners.add(new ListenerBlockChange(this));
        this.listeners.add(new ListenerDamage(this));
        this.setData(new SimpleData(this,
            "Highlights blocks being mined, colored by progress."));
    }

    public enum Mode
    {
        IN,
        OUT
    }

    public enum ColorMode
    {
        PROGRESS,
        CUSTOM
    }
}
