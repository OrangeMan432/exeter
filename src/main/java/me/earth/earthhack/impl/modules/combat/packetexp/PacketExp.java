package me.earth.earthhack.impl.modules.combat.packetexp;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.math.StopWatch;

/**
 * Throws XP via packets while a key or middleclick is held.
 * Ported from Mio 0.6.9 (PacketExp).
 */
public class PacketExp extends Module
{
    protected final Setting<Mode> mode =
        register(new EnumSetting<>("Mode", Mode.KEY));
    protected final Setting<Integer> delay =
        register(new NumberSetting<>("Delay", 1, 0, 5));

    final StopWatch delayTimer = new StopWatch();

    public PacketExp()
    {
        super("PacketExp", Category.Combat);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Packet XP throw on key or middleclick hold."));
    }

    public enum Mode
    {
        KEY,
        MIDDLECLICK
    }
}
