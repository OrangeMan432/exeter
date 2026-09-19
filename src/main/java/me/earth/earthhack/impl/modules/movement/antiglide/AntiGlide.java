package me.earth.earthhack.impl.modules.movement.antiglide;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Kills inertia and optionally ice slipperiness.
 * Ported from Mio 0.6.9 (AntiGlide).
 */
public class AntiGlide extends Module
{
    protected final Setting<Boolean> onGround =
        register(new BooleanSetting("OnGround", true));
    protected final Setting<Boolean> ice =
        register(new BooleanSetting("Ice", true));

    public AntiGlide()
    {
        super("AntiGlide", Category.Movement);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Stops sliding on ground and ice."));
    }

    @Override
    protected void onDisable()
    {
        setIceSlipperiness(0.98f);
    }

    static void setIceSlipperiness(float slipperiness)
    {
        net.minecraft.init.Blocks.ICE.setDefaultSlipperiness(slipperiness);
        net.minecraft.init.Blocks.FROSTED_ICE
            .setDefaultSlipperiness(slipperiness);
        net.minecraft.init.Blocks.PACKED_ICE
            .setDefaultSlipperiness(slipperiness);
    }
}
