package me.earth.earthhack.impl.modules.render.portal;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.block.BlockPortal;
import net.minecraft.util.math.BlockPos;

final class ListenerTick extends ModuleListener<PortalESP, TickEvent>
{
    public ListenerTick(PortalESP module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        if (module.cooldown-- > 0)
        {
            return;
        }

        module.cooldown = 80;
        module.portals.clear();
        int dist = module.distance.getValue();
        int px = (int) mc.player.posX;
        int py = (int) mc.player.posY;
        int pz = (int) mc.player.posZ;
        for (int x = px - dist; x <= px + dist; x++)
        {
            for (int y = Math.max(py - dist, 0);
                 y <= Math.min(py + dist, 255);
                 y++)
            {
                for (int z = Math.max(pz - dist, 0);
                     z <= Math.min(pz + dist, 255);
                     z++)
                {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock()
                            instanceof BlockPortal)
                    {
                        module.portals.add(pos);
                    }
                }
            }
        }
    }
}
