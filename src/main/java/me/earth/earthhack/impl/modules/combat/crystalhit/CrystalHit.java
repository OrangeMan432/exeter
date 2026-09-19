package me.earth.earthhack.impl.modules.combat.crystalhit;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Breaks the nearest crystal, nothing else.
 * Ported from Lemon (CrystalHit).
 */
public class CrystalHit extends Module
{
    protected final Setting<Integer> range =
        register(new NumberSetting<>("Range", 4, 0, 10));
    protected final Setting<Integer> delay =
        register(new NumberSetting<>("Delay", 0, 0, 40));
    protected final Setting<Boolean> swing =
        register(new BooleanSetting("Swing", false));
    protected final Setting<Boolean> packetBreak =
        register(new BooleanSetting("PacketBreak", false));
    protected final Setting<Boolean> antiWeakness =
        register(new BooleanSetting("AntiWeakness", false));

    int delayTime;

    public CrystalHit()
    {
        super("CrystalHit", Category.Combat);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Hits the closest crystal fast."));
    }

    @Override
    protected void onDisable()
    {
        delayTime = 0;
    }
}
