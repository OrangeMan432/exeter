package me.friendly.exeter.beta.mixin;

import me.friendly.exeter.module.impl.toggle.combat.KillAura;
import net.minecraft.class_41;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Compensates movement input while KillAura silently rotates. Beta movement is yaw-relative,
 * so without this WASD walks toward the target instead of the camera direction. Rotates the
 * (forward, strafe) input by aimDelta so world movement matches the camera.
 */
@Mixin(ClientPlayerEntity.class)
public class MixinKillAuraStrafe {

  @Inject(
      method = "method_937()V",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/class_41;method_1942(Lnet/minecraft/entity/player/PlayerEntity;)V",
              shift = At.Shift.AFTER))
  private void compensateInput(CallbackInfo info) {
    if (!KillAura.isAiming()) {
      return;
    }
    ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
    if (player.field_161 == null) {
      return;
    }
    class_41 input = player.field_161;
    float forward = input.field_2533;
    float strafe = input.field_2532;
    if (forward == 0.0F && strafe == 0.0F) {
      return;
    }
    double d = KillAura.aimDelta();
    double cos = Math.cos(d);
    double sin = Math.sin(d);
    input.field_2533 = (float) (forward * cos - strafe * sin);
    input.field_2532 = (float) (forward * sin + strafe * cos);
  }
}
