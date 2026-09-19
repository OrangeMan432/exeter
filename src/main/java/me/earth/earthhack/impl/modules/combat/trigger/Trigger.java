package me.earth.earthhack.impl.modules.combat.trigger;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.math.StopWatch;

/**
 * Clicks entities under the crosshair at set CPS.
 * Ported from Future 2.9 (Trigger).
 */
public class Trigger extends Module
{
    protected final Setting<AttackCheck> attackCheck =
        register(new EnumSetting<>("AttackCheck", AttackCheck.CROSSHAIR));
    protected final Setting<Boolean> invisibleCheck =
        register(new BooleanSetting("InvisibleCheck", true));
    protected final Setting<Boolean> teamCheck =
        register(new BooleanSetting("TeamCheck", false));
    protected final Setting<Boolean> friendCheck =
        register(new BooleanSetting("FriendCheck", true));
    protected final Setting<Boolean> weaponCheck =
        register(new BooleanSetting("WeaponCheck", true));
    protected final Setting<Float> cps =
        register(new NumberSetting<>("CPS", 8.0f, 0.1f, 20.0f));
    protected final Setting<Float> randomSpeed =
        register(new NumberSetting<>("RandomSpeed", 2.0f, 0.1f, 10.0f));

    final StopWatch timer = new StopWatch();

    public Trigger()
    {
        super("Trigger", Category.Combat);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Auto clicks with jittered CPS."));
    }

    public enum AttackCheck
    {
        MOUSE,
        CROSSHAIR,
        ALWAYS
    }
}
