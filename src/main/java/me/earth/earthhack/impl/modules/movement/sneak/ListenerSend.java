package me.earth.earthhack.impl.modules.movement.sneak;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;

final class ListenerSend extends
        ModuleListener<Sneak, PacketEvent.Send<CPacketPlayerTryUseItemOnBlock>>
{
    public ListenerSend(Sneak module)
    {
        super(module,
              PacketEvent.Send.class,
              CPacketPlayerTryUseItemOnBlock.class);
    }

    @Override
    public void invoke(
        PacketEvent.Send<CPacketPlayerTryUseItemOnBlock> event)
    {
        if (mc.player != null
            && module.mode.getValue() != Sneak.Mode.Always
            && !mc.player.isSneaking())
        {
            mc.player.connection.sendPacket(new CPacketEntityAction(
                mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
        }
    }
}
