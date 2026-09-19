package me.earth.earthhack.impl.modules.combat.selfweb;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.math.StopWatch;

/**
 * Places webs on your feet and on close enemies.
 * Ported from Mio 0.6.9 (SelfWeb).
 */
public class SelfWeb extends Module
{
    protected final Setting<Integer> delay =
        register(new NumberSetting<>("Delay", 50, 0, 250));
    protected final Setting<Integer> blocksPerTick =
        register(new NumberSetting<>("BPT", 2, 1, 30));
    protected final Setting<Boolean> packet =
        register(new BooleanSetting("Packet", true));
    protected final Setting<Boolean> noLag =
        register(new BooleanSetting("NoLag", true));
    protected final Setting<Boolean> jumpDisable =
        register(new BooleanSetting("JumpDisable", true));
    protected final Setting<Swap> swap =
        register(new EnumSetting<>("Swap", Swap.NORMAL));
    protected final Setting<Boolean> smart =
        register(new BooleanSetting("Smart", false));
    protected final Setting<Boolean> above =
        register(new BooleanSetting("Above", false));

    final StopWatch delayTimer = new StopWatch();
    final StopWatch lagTimer = new StopWatch();
    int placements;

    public SelfWeb()
    {
        super("SelfWeb", Category.Combat);
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerPosLook(this));
        this.setData(new SimpleData(this,
            "Webs your feet and enemies standing close to you."));
    }

    public enum Swap
    {
        NORMAL,
        PACKET
    }
}
