package me.earth.earthhack.impl.modules.misc.tpcoordlog;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/**
 * Logs players that teleport (render-distance jumps).
 * Ported from Mio 0.6.9 (TPCoordLog).
 */
public class TPCoordLog extends Module
{
    protected final Setting<Boolean> saveToFile =
        register(new BooleanSetting("SaveToFile", true));

    final Map<Entity, Vec3d> knownPlayers = new HashMap<>();
    final Map<String, Vec3d> teleportedPlayers = new HashMap<>();
    int ticks;
    int forgetTicks;

    public TPCoordLog()
    {
        super("TPCoordLog", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.setData(new SimpleData(this,
            "Logs players that teleport far away and optionally saves them to a file."));
    }
}
