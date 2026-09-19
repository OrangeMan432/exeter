package me.earth.earthhack.impl.modules.misc.packetlogger;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts packets by type for bypass debugging.
 * Original Exeter addition, RusherHack-style.
 */
public class PacketLogger extends Module
{
    protected final Setting<Boolean> incoming =
        register(new BooleanSetting("Incoming", true));
    protected final Setting<Boolean> outgoing =
        register(new BooleanSetting("Outgoing", true));
    protected final Setting<Integer> topCount =
        register(new NumberSetting<>("Top", 10, 1, 30));

    final Map<String, Integer> inCounts = new ConcurrentHashMap<>();
    final Map<String, Integer> outCounts = new ConcurrentHashMap<>();

    public PacketLogger()
    {
        super("PacketLogger", Category.Misc);
        this.listeners.add(new ListenerSend(this));
        this.listeners.add(new ListenerReceive(this));
        this.setData(new SimpleData(this,
            "Logs packet counts, find what flags you."));
    }

    @Override
    protected void onDisable()
    {
        inCounts.clear();
        outCounts.clear();
    }
}
