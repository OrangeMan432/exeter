package me.earth.earthhack.impl.modules.movement.holetp;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.movement.speed.Speed;
import me.earth.earthhack.impl.modules.movement.strafe.Strafe;
import me.earth.earthhack.impl.util.client.SimpleData;

/**
 * Packet-teleports you into holes you stand over.
 * Ported from Phobos 1.9 (HoleTP).
 */
public class HoleTP extends Module
{
    private static final ModuleCache<Speed> SPEED =
            Caches.getModule(Speed.class);
    private static final ModuleCache<Strafe> STRAFE =
            Caches.getModule(Strafe.class);

    final double[] oneBlockPositions = new double[]{0.42, 0.75};
    int packets;
    boolean jumped;

    public HoleTP()
    {
        super("HoleTP", Category.Movement);
        this.listeners.add(new ListenerMotion(this));
        this.setData(new SimpleData(this,
            "Teleports you down into holes."));
    }

    boolean shouldReturn()
    {
        return SPEED.isEnabled() || STRAFE.isEnabled();
    }
}
