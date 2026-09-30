package me.friendly.exeter.beta.mixin;

import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.class_119;
import net.minecraft.class_382;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels server entity-velocity packets for the player while Velocity is enabled.
 * class_119 is the entity velocity packet (entity id + velocities / 8000);
 * method_1443 applies it via Entity.method_1365.
 */
@Mixin(ClientNetworkHandler.class)
public class MixinVelocity {

  @Inject(method = "method_1443(Lnet/minecraft/class_119;)V", at = @At("HEAD"), cancellable = true)
  private void onEntityVelocity(class_119 packet, CallbackInfo info) {
    if (Exeter.getInstance() == null) {
      return;
    }
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("velocity");
    if (!(module instanceof ToggleableModule) || !((ToggleableModule) module).isRunning()) {
      return;
    }
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null) {
      return;
    }
    // field_364 is the entity id.
    if (packet.field_364 == mc.player.id) {
      DebugLogger.get()
          .logSystem(
              "Velocity",
              "Canceled entity velocity "
                  + packet.field_365
                  + "/"
                  + packet.field_366
                  + "/"
                  + packet.field_367);
      info.cancel();
    }
  }

  private double savedVelX;
  private double savedVelY;
  private double savedVelZ;
  private boolean savedBlast;

  private boolean velocityRunning() {
    if (Exeter.getInstance() == null) {
      return false;
    }
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("velocity");
    return module instanceof ToggleableModule && ((ToggleableModule) module).isRunning();
  }

  /**
   * Explosions apply player knockback client-side from the explosion packet, so there is
   * no velocity packet to cancel. Snapshot the player velocity around it instead;
   * particles, sound and block effects still play.
   */
  @Inject(method = "method_1458(Lnet/minecraft/class_382;)V", at = @At("HEAD"))
  private void onExplosionStart(class_382 packet, CallbackInfo info) {
    savedBlast = false;
    if (!velocityRunning()) {
      return;
    }
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null) {
      return;
    }
    savedVelX = mc.player.velocityX;
    savedVelY = mc.player.velocityY;
    savedVelZ = mc.player.velocityZ;
    savedBlast = true;
  }

  @Inject(method = "method_1458(Lnet/minecraft/class_382;)V", at = @At("RETURN"))
  private void onExplosionEnd(class_382 packet, CallbackInfo info) {
    if (!savedBlast) {
      return;
    }
    savedBlast = false;
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null) {
      return;
    }
    mc.player.velocityX = savedVelX;
    mc.player.velocityY = savedVelY;
    mc.player.velocityZ = savedVelZ;
    DebugLogger.get().logSystem("Velocity", "Nulled explosion knockback");
  }
}
