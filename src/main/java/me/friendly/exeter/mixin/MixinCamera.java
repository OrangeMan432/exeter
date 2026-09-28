package me.friendly.exeter.mixin;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.toggle.render.FreeLook;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies FreeLook's detached camera angles inside alignWithEntity, before vanilla computes the
 * third-person pullback and the cull frustum, so position and chunk culling follow the freelook
 * angles instead of the player's.
 */
@Mixin(Camera.class)
public abstract class MixinCamera {

  @Shadow
  protected abstract void setRotation(float yRot, float xRot);

  @Shadow
  protected abstract void setPosition(Vec3 position);

  @Shadow
  protected abstract void move(float x, float y, float z);

  @Shadow
  private float getMaxZoom(float desiredDistance) {
    return desiredDistance;
  }

  @Shadow private Entity entity;

  @Shadow
  public abstract Vec3 position();

  @Shadow
  public abstract boolean isDetached();

  @Inject(method = "alignWithEntity(F)V", at = @At("TAIL"))
  private void applyFreeLook(float partialTicks, CallbackInfo info) {
    if (Exeter.getInstance() == null) {
      return;
    }
    var module = Exeter.getInstance().getModuleManager().getModuleByAlias("freelook");
    if (!(module instanceof FreeLook freeLook) || !freeLook.isFreeLooking()) {
      return;
    }
    setRotation(freeLook.getCameraYaw(), freeLook.getCameraPitch());
    if (isDetached() && entity != null) {
      // Redo vanilla's pullback along the freelook angles: reset to the eye, then move back
      // by the same collision-aware distance vanilla just used.
      Vec3 eye = entity.getEyePosition(partialTicks);
      float distance = (float) eye.distanceTo(position());
      setPosition(eye);
      move(-getMaxZoom(distance), 0.0f, 0.0f);
    }
  }
}
