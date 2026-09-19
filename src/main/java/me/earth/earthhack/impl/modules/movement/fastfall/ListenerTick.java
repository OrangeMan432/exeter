package me.earth.earthhack.impl.modules.movement.fastfall;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.BlockPos;

final class ListenerTick extends ModuleListener<FastFall, TickEvent>
{
    public ListenerTick(FastFall module)
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

        if (mc.player.onGround
            || mc.player.isElytraFlying()
            || isAboveWater()
            || !module.lagTimer.passed(1000)
                && module.noLag.getValue())
        {
            Managers.TIMER.reset();
            return;
        }

        if (module.mode.getValue() == FastFall.Mode.Timer
            || module.mode.getValue() == FastFall.Mode.All)
        {
            Managers.TIMER.setTimer(module.fallSpeed.getValue());
        }

        if (module.mode.getValue() == FastFall.Mode.Packet
            || module.mode.getValue() == FastFall.Mode.All)
        {
            BlockPos ground = traceGround();
            if (ground != null
                && mc.player.posY - ground.getY() - 1.0
                    > module.height.getValue())
            {
                mc.player.connection.sendPacket(
                    new CPacketPlayer.Position(mc.player.posX,
                                               0.0,
                                               mc.player.posZ,
                                               mc.player.onGround));
                mc.player.setPosition(mc.player.posX,
                                      ground.getY() + 1.0,
                                      mc.player.posZ);
            }
        }
    }

    private boolean isAboveWater()
    {
        BlockPos ground = traceGround();
        return ground != null
            && mc.world.getBlockState(ground).getBlock() == Blocks.WATER;
    }

    private BlockPos traceGround()
    {
        BlockPos playerPos = new BlockPos(mc.player.posX,
                                          mc.player.posY,
                                          mc.player.posZ);
        for (int y = playerPos.getY() - 2; y > 0; y--)
        {
            BlockPos pos = new BlockPos(playerPos.getX(), y, playerPos.getZ());
            if (mc.world.getBlockState(pos).getMaterial().isSolid()
                || mc.world.getBlockState(pos).getBlock() == Blocks.WATER)
            {
                return pos;
            }
        }

        return null;
    }
}
