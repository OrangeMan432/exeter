package me.earth.earthhack.impl.modules.misc.packetcanceller;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketInput;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerAbilities;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.client.CPacketVehicleMove;

final class ListenerSend extends
        ModuleListener<PacketCanceller, PacketEvent.Send<?>>
{
    public ListenerSend(PacketCanceller module)
    {
        super(module, PacketEvent.Send.class);
    }

    @Override
    public void invoke(PacketEvent.Send<?> event)
    {
        Packet<?> packet = event.getPacket();
        if (packet instanceof CPacketInput && module.input.getValue()
            || packet instanceof CPacketPlayer.Position
                && module.position.getValue()
            || packet instanceof CPacketPlayer.PositionRotation
                && module.positionRotation.getValue()
            || packet instanceof CPacketPlayer.Rotation
                && module.rotation.getValue()
            || packet instanceof CPacketPlayerAbilities
                && module.abilities.getValue()
            || packet instanceof CPacketPlayerDigging
                && module.digging.getValue()
            || packet instanceof CPacketPlayerTryUseItem
                && module.useItem.getValue()
            || packet instanceof CPacketPlayerTryUseItemOnBlock
                && module.useItemOnBlock.getValue()
            || packet instanceof CPacketEntityAction
                && module.entityAction.getValue()
            || packet instanceof CPacketUseEntity
                && module.useEntity.getValue()
            || packet instanceof CPacketVehicleMove
                && module.vehicleMove.getValue())
        {
            module.cancelled++;
            event.setCancelled(true);
        }
    }
}
