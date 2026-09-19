package me.earth.earthhack.impl.modules.player.keypearl;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Throws pearls on key or middleclick, skipping players.
 * Ported from Mio 0.6.9 (KeyPearl).
 */
public class KeyPearl extends Module
{
    protected final Setting<Mode> mode =
        register(new EnumSetting<>("Mode", Mode.MIDDLECLICK));
    protected final Setting<Boolean> noPlayerTrace =
        register(new BooleanSetting("NoPlayerTrace", true));

    boolean clicked;

    public KeyPearl()
    {
        super("KeyPearl", Category.Player);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Throws a pearl without hitting friends."));
    }

    public enum Mode
    {
        KEY,
        MIDDLECLICK
    }
}
