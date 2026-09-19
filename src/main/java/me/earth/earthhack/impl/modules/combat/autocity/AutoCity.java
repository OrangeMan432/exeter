package me.earth.earthhack.impl.modules.combat.autocity;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.util.math.BlockPos;

/**
 * Automatically mines city blocks around holed enemies.
 * Inspired by SalHack (AutoCity), rewritten for this base.
 */
public class AutoCity extends Module
{
    protected final Setting<Double> range =
        register(new NumberSetting<>("Range", 6.0, 1.0, 12.0));
    protected final Setting<Boolean> autoSwitch =
        register(new BooleanSetting("AutoSwitch", true));
    protected final Setting<Boolean> autoDisable =
        register(new BooleanSetting("AutoDisable", true));

    BlockPos current;
    int lastSlot = -1;

    public AutoCity()
    {
        super("AutoCity", Category.Combat);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Mines the obsidian enemies hide behind."));
    }

    @Override
    protected void onDisable()
    {
        current = null;
        if (mc.player != null
            && lastSlot != -1
            && lastSlot != mc.player.inventory.currentItem)
        {
            mc.player.inventory.currentItem = lastSlot;
        }

        lastSlot = -1;
    }
}
