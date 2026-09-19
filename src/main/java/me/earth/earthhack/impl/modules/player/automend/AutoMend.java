package me.earth.earthhack.impl.modules.player.automend;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Auto-throws XP when armor durability drops below threshold.
 * Original Exeter addition for the 1.12.2 branch.
 */
public class AutoMend extends Module
{
    protected final Setting<Integer> threshold =
        register(new NumberSetting<>("Threshold%", 80, 1, 100));
    protected final Setting<Boolean> autoSwitch =
        register(new BooleanSetting("AutoSwitch", true));
    protected final Setting<Boolean> autoThrow =
        register(new BooleanSetting("AutoThrow", true));
    protected final Setting<Boolean> autoDisable =
        register(new BooleanSetting("AutoDisable", false));

    int initSlot = -1;
    boolean mending;

    public AutoMend()
    {
        super("AutoMend", Category.Player);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Mends armor with XP bottles when durability is low."));
    }

    @Override
    protected void onEnable()
    {
        initSlot = mc.player == null
            ? -1
            : mc.player.inventory.currentItem;
        mending = false;
    }

    @Override
    protected void onDisable()
    {
        if (mc.player != null
            && initSlot != -1
            && initSlot != mc.player.inventory.currentItem)
        {
            mc.player.inventory.currentItem = initSlot;
        }

        mending = false;
    }
}
