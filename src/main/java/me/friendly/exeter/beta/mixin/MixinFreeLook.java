package me.friendly.exeter.beta.mixin;

import net.minecraft.class_555;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.friendly.exeter.module.impl.toggle.render.FreeLook;

/**
 * Hooks the beta renderer (class_555) for FreeLook. method_1844 applies the vanilla mouse
 * turn via ClientPlayerEntity.method_1362; method_1851 orients the camera from player angles.
 */
@Mixin(class_555.class)
public class MixinFreeLook {

  @Inject(
      method = "method_1844(F)V",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/entity/player/ClientPlayerEntity;method_1362(FF)V",
              shift = At.Shift.BEFORE))
  private void recordRotation(float tickDelta, CallbackInfo info) {
    if (!FreeLook.isFreeLooking()) {
      return;
    }
    FreeLook look = FreeLook.get();
    if (look != null) {
      look.recordPlayerRotation();
    }
  }

  @Inject(
      method = "method_1844(F)V",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/entity/player/ClientPlayerEntity;method_1362(FF)V",
              shift = At.Shift.AFTER))
  private void redirectRotation(float tickDelta, CallbackInfo info) {
    if (!FreeLook.isFreeLooking()) {
      return;
    }
    FreeLook look = FreeLook.get();
    if (look != null) {
      look.redirectTurn();
    }
  }

  @Inject(method = "method_1851(F)V", at = @At("HEAD"))
  private void swapCamera(float tickDelta, CallbackInfo info) {
    if (!FreeLook.isFreeLooking()) {
      return;
    }
    FreeLook look = FreeLook.get();
    if (look != null) {
      look.swapToCamera();
    }
  }

  @Inject(method = "method_1851(F)V", at = @At("RETURN"))
  private void restoreCamera(float tickDelta, CallbackInfo info) {
    if (!FreeLook.isFreeLooking()) {
      return;
    }
    FreeLook look = FreeLook.get();
    if (look != null) {
      look.restorePlayer();
    }
  }
}
