package me.earth.earthhack.impl.modules.misc.unfocusedcpu;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import org.lwjgl.opengl.Display;

/**
 * Drops FPS while the window is unfocused.
 * Ported from Mio 0.6.9 (UnfocusedCPU).
 */
public class UnfocusedCPU extends Module
{
    protected final Setting<Integer> fps =
        register(new NumberSetting<>("FPS", 1, 1, 60));

    public UnfocusedCPU()
    {
        super("UnfocusedCPU", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Saves CPU when tabbed out."));
    }
}
