package me.earth.earthhack.impl.modules.player.fastexp;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Auto-switches to XP bottles and throws them without delay.
 * Ported from Kami Blue (FastExp).
 */
public class FastExp extends Module
{
    protected final Setting<Boolean> autoThrow =
        register(new BooleanSetting("AutoThrow", true));
    protected final Setting<Boolean> autoSwitch =
        register(new BooleanSetting("AutoSwitch", true));
    protected final Setting<Boolean> autoDisable =
        register(new BooleanSetting("AutoDisable", false));

    int initSlot = -1;

    public FastExp()
    {
        super("FastExp", Category.Player);
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerReceive(this));
        this.setData(new SimpleData(this,
            "Switches to XP and throws it with no use delay."));
    }

    @Override
    protected void onEnable()
    {
        initSlot = mc.player == null
            ? -1
            : mc.player.inventory.currentItem;
    }

    @Override
    protected void onDisable()
    {
        if (mc.player != null
            && autoSwitch.getValue()
            && initSlot != -1
            && initSlot != mc.player.inventory.currentItem)
        {
            mc.player.inventory.currentItem = initSlot;
        }
    }
}
