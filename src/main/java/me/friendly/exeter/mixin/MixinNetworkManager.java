package me.friendly.exeter.mixin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class is not present in the original Exeter 1.8 client. It was added as part of the Fabric
 * 1.21.11 port
 *
 * @author Gopro336
 */
@Mixin(value = Connection.class)
public class MixinNetworkManager {

  @Inject(
      method =
          "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
      at = @At("HEAD"),
      cancellable = true)
  public void onPacketSend(
      Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo info) {
    PacketEvent packetSendEvent = new PacketEvent(packet);
    Exeter.getInstance().getEventManager().dispatch(packetSendEvent);

    if (packetSendEvent.isCanceled()) {
      info.cancel();
    }
  }

  @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
  public void onPacketReceive(ChannelHandlerContext chc, Packet<?> packet, CallbackInfo info) {
    PacketEvent packetReceiveEvent = new PacketEvent(packet);
    Exeter.getInstance().getEventManager().dispatch(packetReceiveEvent);

    if (packetReceiveEvent.isCanceled()) {
      info.cancel();
    }
  }
}
