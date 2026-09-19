package me.earth.earthhack.impl.modules.misc.packetfilter;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.network.play.server.SPacketEntityEffect;
import net.minecraft.network.play.server.SPacketEntityMetadata;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.network.play.server.SPacketKeepAlive;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.network.play.server.SPacketSpawnPlayer;
import net.minecraft.network.play.server.SPacketTimeUpdate;

final class ListenerReceive extends
        ModuleListener<PacketFilter, PacketEvent.Receive<?>>
{
    public ListenerReceive(PacketFilter module)
    {
        super(module, PacketEvent.Receive.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<?> event)
    {
        Packet<?> packet = event.getPacket();
        if (packet instanceof SPacketExplosion && module.explosion.getValue()
            || packet instanceof SPacketEntityVelocity
                && module.entityVelocity.getValue()
            || packet instanceof SPacketPlayerPosLook
                && module.posLook.getValue()
            || packet instanceof SPacketEntityMetadata
                && module.entityMetadata.getValue()
            || packet instanceof SPacketBlockChange
                && module.blockChange.getValue()
            || packet instanceof SPacketSoundEffect
                && module.soundEffect.getValue()
            || packet instanceof SPacketChat && module.chat.getValue()
            || packet instanceof SPacketKeepAlive
                && module.keepAlive.getValue()
            || packet instanceof SPacketEntityEffect
                && module.entityEffect.getValue()
            || packet instanceof SPacketDestroyEntities
                && module.destroyEntities.getValue()
            || packet instanceof SPacketSpawnPlayer
                && module.spawnPlayer.getValue()
            || packet instanceof SPacketTimeUpdate
                && module.timeUpdate.getValue())
        {
            module.cancelled++;
            event.setCancelled(true);
        }
    }
}
