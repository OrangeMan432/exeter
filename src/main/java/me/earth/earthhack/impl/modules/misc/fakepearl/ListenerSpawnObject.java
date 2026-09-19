package me.earth.earthhack.impl.modules.misc.fakepearl;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketSpawnObject;

import java.util.Comparator;

final class ListenerSpawnObject extends
        ModuleListener<FakePearl, PacketEvent.Receive<SPacketSpawnObject>>
{
    public ListenerSpawnObject(FakePearl module)
    {
        super(module, PacketEvent.Receive.class, SPacketSpawnObject.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketSpawnObject> event)
    {
        if (mc.world == null || mc.player == null)
        {
            return;
        }

        SPacketSpawnObject packet = event.getPacket();
        if (packet.getType() != 65)
        {
            return;
        }

        mc.world.playerEntities
            .stream()
            .min(Comparator.comparingDouble(
                player -> player.getDistance(
                    packet.getX(), packet.getY(), packet.getZ())))
            .ifPresent(player ->
            {
                if (!player.equals(mc.player) || !mc.player.onGround)
                {
                    return;
                }

                mc.player.motionX = 0.0;
                mc.player.motionY = 0.0;
                mc.player.motionZ = 0.0;
                mc.player.movementInput.moveForward = 0.0f;
                mc.player.movementInput.moveStrafe = 0.0f;
                mc.player.connection.sendPacket(new CPacketPlayer.Position(
                    mc.player.posX, mc.player.posY + 1.0, mc.player.posZ, false));
                module.thrownPearlId = packet.getEntityID();
            });
    }
}
