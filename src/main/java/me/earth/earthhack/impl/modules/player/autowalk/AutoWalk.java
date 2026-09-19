package me.earth.earthhack.impl.modules.player.autowalk;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Walks forward or backwards for you.
 * Ported from Kami Blue (AutoWalk, walk modes).
 */
public class AutoWalk extends Module
{
    protected final Setting<Mode> mode =
        register(new EnumSetting<>("Mode", Mode.FORWARD));

    public AutoWalk()
    {
        super("AutoWalk", Category.Player);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Walks without holding keys."));
    }

    @Override
    protected void onDisable()
    {
        if (mc.gameSettings != null)
        {
            mc.gameSettings.keyBindForward.pressed = false;
            mc.gameSettings.keyBindBack.pressed = false;
        }
    }

    public enum Mode
    {
        FORWARD,
        BACKWARDS
    }
}
