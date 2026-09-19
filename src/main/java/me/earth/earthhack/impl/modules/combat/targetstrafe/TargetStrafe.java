package me.earth.earthhack.impl.modules.combat.targetstrafe;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Orbits your closest enemy automatically.
 * Original Exeter addition for the 1.12.2 branch.
 */
public class TargetStrafe extends Module
{
    protected final Setting<Double> range =
        register(new NumberSetting<>("Range", 3.0, 0.5, 8.0));
    protected final Setting<Double> speed =
        register(new NumberSetting<>("Speed", 0.3, 0.05, 1.0));
    protected final Setting<Boolean> clockwise =
        register(new BooleanSetting("Clockwise", true));
    protected final Setting<Boolean> jump =
        register(new BooleanSetting("Jump", false));

    EntityPlayer target;

    public TargetStrafe()
    {
        super("TargetStrafe", Category.Combat);
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerMove(this));
        this.setData(new SimpleData(this,
            "Strafes around enemies in range."));
    }
}
