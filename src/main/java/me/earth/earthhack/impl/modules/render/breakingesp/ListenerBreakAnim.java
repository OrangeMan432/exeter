package me.earth.earthhack.impl.modules.render.breakingesp;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.SPacketBlockBreakAnim;
import net.minecraft.util.math.BlockPos;

final class ListenerBreakAnim extends
        ModuleListener<BreakingESP, PacketEvent.Receive<SPacketBlockBreakAnim>>
{
    public ListenerBreakAnim(BreakingESP module)
    {
        super(module, PacketEvent.Receive.class, SPacketBlockBreakAnim.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketBlockBreakAnim> event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        SPacketBlockBreakAnim packet = event.getPacket();
        BlockPos pos = packet.getPosition();
        if (mc.world.getBlockState(pos).getBlock() == Blocks.AIR
            || mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK
            || mc.world.getBlockState(pos).getBlock() == Blocks.BARRIER
            || module.animTicks.containsKey(pos))
        {
            return;
        }

        if (!module.showSelf.getValue()
            && mc.world.getEntityByID(packet.getBreakerId()) == mc.player)
        {
            return;
        }

        module.animTicks.put(pos, 0);
    }
}
