package me.earth.earthhack.impl.modules.movement.yaw;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Locks yaw, pitch or cardinal direction.
 * Ported from SalHack (Yaw).
 */
public class Yaw extends Module
{
    protected final Setting<Boolean> yawLock =
        register(new BooleanSetting("Yaw", true));
    protected final Setting<Boolean> pitchLock =
        register(new BooleanSetting("Pitch", false));
    protected final Setting<Boolean> cardinal =
        register(new BooleanSetting("Cardinal", false));

    float yaw;
    float pitch;

    public Yaw()
    {
        super("Yaw", Category.Movement);
        this.listeners.add(new ListenerMotion(this));
        this.setData(new SimpleData(this,
            "Locks rotation for precise building."));
    }

    @Override
    protected void onEnable()
    {
        if (mc.player != null)
        {
            yaw = mc.player.rotationYaw;
            pitch = mc.player.rotationPitch;
        }
    }
}
