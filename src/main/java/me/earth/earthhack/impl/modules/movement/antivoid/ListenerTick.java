package me.earth.earthhack.impl.modules.movement.antivoid;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

final class ListenerTick extends ModuleListener<AntiVoid, TickEvent>
{
    public ListenerTick(AntiVoid module)
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

        boolean voidBelow = true;
        for (int y = (int) mc.player.posY; y > -1; y--)
        {
            if (mc.world.getBlockState(
                    new BlockPos(mc.player.posX, y, mc.player.posZ))
                    .getBlock() != Blocks.AIR)
            {
                voidBelow = false;
                break;
            }
        }

        if (voidBelow && mc.player.posY < module.height.getValue())
        {
            mc.player.motionY = 0.0;
        }
    }
}
