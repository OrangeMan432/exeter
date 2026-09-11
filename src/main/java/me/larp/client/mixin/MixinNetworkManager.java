package me.larp.client.mixin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import me.larp.client.core.Larp;
import me.larp.client.events.PacketEvent;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class is not present in the original Larp 1.8 client. It was added as part of the Fabric
 * 1.21.11 port
 *
 * @author Gopro336
 */
@Mixin(value = Connection.class)
public class MixinNetworkManager {

  @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
  public void onPacketSend(
      Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo info) {
    PacketEvent packetSendEvent = new PacketEvent(packet);
    Larp.getInstance().getEventManager().dispatch(packetSendEvent);

    if (packetSendEvent.isCanceled()) {
      info.cancel();
    }
  }

  @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
  public void onPacketReceive(ChannelHandlerContext chc, Packet<?> packet, CallbackInfo info) {
    PacketEvent packetReceiveEvent = new PacketEvent(packet);
    Larp.getInstance().getEventManager().dispatch(packetReceiveEvent);

    if (packetReceiveEvent.isCanceled()) {
      info.cancel();
    }
  }
}
