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
 * This class is not present in the original
 * Exeter 1.8 client. It was added as part
 * of the Fabric 1.21.11 port
 *
 * @author Gopro336
 */
@Mixin(value = Connection.class)
public class MixinNetworkManager {

    /**
     * Re-entrancy guard: when a listener replaces a packet we re-send the replacement through
     * the public {@code send} path, which funnels back into this same private method. The guard
     * lets our own re-send pass through untouched (no second dispatch, no infinite loop).
     */
    private static final ThreadLocal<Boolean> exeter$replacing = ThreadLocal.withInitial(() -> false);

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    public void onPacketSend(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo info) {
        if (Exeter.getInstance() == null) return;
        if (exeter$replacing.get()) return; // our own re-send of a replacement packet

        PacketEvent packetSendEvent = new PacketEvent(packet);
        Exeter.getInstance().getEventManager()
                .dispatch(packetSendEvent);

        if (packetSendEvent.isCanceled()) {
            info.cancel();
            return;
        }

        // Honor packet replacement (e.g. AntiAim swapping in a scrambled move packet):
        // cancel the original and send the replacement through the public send path.
        Packet<?> replacement = packetSendEvent.getPacket();
        if (replacement != null && replacement != packet) {
            info.cancel();
            exeter$replacing.set(true);
            try {
                ((Connection) (Object) this).send(replacement, listener, flush);
            } finally {
                exeter$replacing.set(false);
            }
        }
    }

    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    public void onPacketReceive(ChannelHandlerContext chc, Packet<?> packet, CallbackInfo info) {
        PacketEvent packetReceiveEvent = new PacketEvent(packet);
        Exeter.getInstance().getEventManager()
                .dispatch(packetReceiveEvent);

        if (packetReceiveEvent.isCanceled()) {
            info.cancel();
        }
    }
}
