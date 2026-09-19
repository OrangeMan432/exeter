package me.earth.earthhack.impl.modules.combat.burrow;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Self-fills obsidian with high rubberband packets.
 * Rubberband bypass pattern from the mioclient burrow-bypass note.
 */
public class Burrow extends Module
{
    protected final Setting<Boolean> rotate =
        register(new BooleanSetting("Rotate", false));
    protected final Setting<Integer> risePackets =
        register(new NumberSetting<>("RisePackets", 20, 1, 40));
    protected final Setting<Double> riseHeight =
        register(new NumberSetting<>("RiseHeight", 1337.0, 100.0, 2000.0));
    protected final Setting<Boolean> autoDisable =
        register(new BooleanSetting("AutoDisable", true));

    public Burrow()
    {
        super("Burrow", Category.Combat);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Burrows you with rubberband bypass packets."));
    }
}
