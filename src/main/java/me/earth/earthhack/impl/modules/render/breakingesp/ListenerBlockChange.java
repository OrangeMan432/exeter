package me.earth.earthhack.impl.modules.render.breakingesp;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.SPacketBlockChange;

final class ListenerBlockChange extends
        ModuleListener<BreakingESP, PacketEvent.Receive<SPacketBlockChange>>
{
    public ListenerBlockChange(BreakingESP module)
    {
        super(module, PacketEvent.Receive.class, SPacketBlockChange.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketBlockChange> event)
    {
        SPacketBlockChange packet = event.getPacket();
        if (module.blocks.containsKey(packet.getBlockPosition())
            && packet.getBlockState().getBlock() != Blocks.AIR)
        {
            module.blocks.remove(packet.getBlockPosition());
        }

        module.animTicks.remove(packet.getBlockPosition());
    }
}
