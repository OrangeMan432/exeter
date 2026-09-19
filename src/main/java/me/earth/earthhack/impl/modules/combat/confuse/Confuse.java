package me.earth.earthhack.impl.modules.combat.confuse;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Spins server rotations to break enemy aim.
 * Original Exeter addition for the 1.12.2 branch.
 */
public class Confuse extends Module
{
    protected final Setting<Float> spinSpeed =
        register(new NumberSetting<>("SpinSpeed", 20.0f, 1.0f, 100.0f));

    float yaw;

    public Confuse()
    {
        super("Confuse", Category.Combat);
        this.listeners.add(new ListenerMotion(this));
        this.setData(new SimpleData(this,
            "Spins you server-side to dodge crystals."));
    }

    @Override
    protected void onEnable()
    {
        yaw = mc.player == null ? 0.0f : mc.player.rotationYaw;
    }
}
