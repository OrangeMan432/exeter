package me.friendly.exeter.beta.mixin;

import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.impl.toggle.movement.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.class_119;
import net.minecraft.class_60;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
    if (!Velocity.isActive()) {
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

  /**
   * Local knockback (singleplayer mob attacks and anything else applied without packets).
   * Cancels LivingEntity knockback for the player while Velocity is enabled.
   */
  @Mixin(LivingEntity.class)
  public abstract static class LocalKnockback {

    @Inject(
        method = "method_925(Lnet/minecraft/entity/Entity;IDD)V",
        at = @At("HEAD"),
        cancellable = true)
    private void onKnockback(Entity attacker, int damage, double x, double z, CallbackInfo info) {
      if (!Velocity.isActive()) {
        return;
      }
      Minecraft mc = MinecraftAccessor.getMinecraft();
      if (mc == null || mc.player == null) {
        return;
      }
      if ((Object) this == mc.player) {
        DebugLogger.get().logSystem("Velocity", "Canceled local knockback");
        info.cancel();
      }
    }
  }

  /**
   * Explosions apply player knockback inside the shared explosion routine, reached from the
   * explosion packet in multiplayer and directly in singleplayer. Snapshot the player
   * velocity around it instead; particles, sound and block effects still play.
   */
  @Mixin(class_60.class)
  public abstract static class ExplosionKnockback {

    @Unique private double savedVelX;
    @Unique private double savedVelY;
    @Unique private double savedVelZ;
    @Unique private boolean savedBlast;

    @Inject(method = "method_1196(Z)V", at = @At("HEAD"))
    private void onExplosionStart(boolean particles, CallbackInfo info) {
      savedBlast = false;
      if (!Velocity.isActive()) {
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

    @Inject(method = "method_1196(Z)V", at = @At("RETURN"))
    private void onExplosionEnd(boolean particles, CallbackInfo info) {
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
}
