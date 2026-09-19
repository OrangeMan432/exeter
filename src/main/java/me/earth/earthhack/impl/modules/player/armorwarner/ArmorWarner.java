package me.earth.earthhack.impl.modules.player.armorwarner;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Warns when your or a friends armor is about to break.
 * Ported from Mio 0.6.9 (ArmorWarner).
 */
public class ArmorWarner extends Module
{
    protected final Setting<Integer> threshold =
        register(new NumberSetting<>("Armor%", 20, 1, 100));
    protected final Setting<Boolean> notifySelf =
        register(new BooleanSetting("Self", true));
    protected final Setting<Boolean> notifyFriends =
        register(new BooleanSetting("Friends", true));

    final Map<EntityPlayer, Integer> warnedPieces = new HashMap<>();

    public ArmorWarner()
    {
        super("ArmorWarner", Category.Player);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Notifies when your or a friends armor is low durability."));
    }
}
