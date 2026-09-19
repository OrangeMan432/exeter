package me.earth.earthhack.impl.modules.combat.webtrap;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.SPacketBlockChange;

final class ListenerBlockChange extends
        ModuleListener<WebTrap, PacketEvent.Receive<SPacketBlockChange>>
{
    public ListenerBlockChange(WebTrap module)
    {
        super(module, PacketEvent.Receive.class, SPacketBlockChange.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketBlockChange> event)
    {
        SPacketBlockChange packet = event.getPacket();
        if (module.renderBlocks.containsKey(packet.getBlockPosition())
            && packet.getBlockState().getBlock() != Blocks.AIR)
        {
            module.renderBlocks.remove(packet.getBlockPosition());
        }
    }
}
