package me.earth.earthhack.impl.modules.misc.fakepearl;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.impl.util.client.SimpleData;
import net.minecraft.network.play.client.CPacketPlayer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Suspends movement packets while your pearl travels (fake pearl).
 * Ported from Mio 0.6.9 (FakePearl).
 */
public class FakePearl extends Module
{
    final Queue<CPacketPlayer> packets = new ConcurrentLinkedQueue<>();
    int thrownPearlId = -1;

    public FakePearl()
    {
        super("FakePearl", Category.Misc);
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerSpawnObject(this));
        this.listeners.add(new ListenerSend(this));
        this.setData(new SimpleData(this,
            "Chokes movement packets while your pearl is flying."));
    }

    @Override
    protected void onDisable()
    {
        packets.clear();
        thrownPearlId = -1;
    }
}
