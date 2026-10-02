package me.friendly.exeter.mixin;

import io.netty.channel.Channel;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.proxy.ProxyEntry;
import me.friendly.exeter.proxy.ProxyManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prepends a SOCKS5/HTTP proxy handler to every game connection when a proxy is active. Netty
 * diverts the connect through the proxy internally, so the vanilla address and pipeline setup stay
 * untouched.
 */
@Mixin(targets = "net.minecraft.network.Connection$1")
public class MixinProxyInitializer {

  @Inject(method = "initChannel", at = @At("HEAD"))
  private void prependProxy(Channel channel, CallbackInfo info) {
    if (Exeter.getInstance() == null) {
      return;
    }
    ProxyManager manager = Exeter.getInstance().getProxyManager();
    if (manager == null) {
      return;
    }
    ProxyEntry active = manager.getActive();
    if (active == null) {
      return;
    }
    if (active.getType() == ProxyEntry.Type.HTTP) {
      if (active.hasAuth()) {
        channel
            .pipeline()
            .addFirst(
                new HttpProxyHandler(active.address(), active.getUsername(), active.getPassword()));
      } else {
        channel.pipeline().addFirst(new HttpProxyHandler(active.address()));
      }
    } else {
      if (active.hasAuth()) {
        channel
            .pipeline()
            .addFirst(
                new Socks5ProxyHandler(
                    active.address(), active.getUsername(), active.getPassword()));
      } else {
        channel.pipeline().addFirst(new Socks5ProxyHandler(active.address()));
      }
    }
  }
}
