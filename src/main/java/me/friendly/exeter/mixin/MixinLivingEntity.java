package me.friendly.exeter.mixin;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TravelEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class)
public class MixinLivingEntity {

  @Inject(method = "travel", at = @At("RETURN"))
  public void onTravelReturn(Vec3 movementInput, CallbackInfo ci) {
    LivingEntity self = (LivingEntity) (Object) this;
    if (self instanceof net.minecraft.client.player.LocalPlayer) {
      Exeter.getInstance().getEventManager().dispatch(new TravelEvent());
    }
  }

  /**
   * Swing length override for the Swing module: vanilla duration divided by the speed setting.
   * Applies to every entity.
   */
  @Inject(
      method = "getModifiedSwingDuration(Lnet/minecraft/world/item/component/SwingAnimation;)I",
      at = @At("RETURN"),
      cancellable = true)
  private void scaleSwingDuration(
      net.minecraft.world.item.component.SwingAnimation animation,
      CallbackInfoReturnable<Integer> cir) {
    float speed = me.friendly.exeter.module.impl.toggle.misc.Swing.swingSpeed();
    if (speed != 1.0f) {
      cir.setReturnValue((int) Math.floor(cir.getReturnValue() / speed));
    }
  }

  /**
   * Old style for the Swing module: an in-progress swing never restarts, so rapid attacks ride one
   * smooth animation. Client-visual only (this version has no client-to-server swing packet),
   * scoped to our own player.
   */
  @Inject(
      method =
          "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z",
      at = @At("HEAD"),
      cancellable = true)
  private void keepSwingPlaying(
      net.minecraft.world.InteractionHand hand,
      net.minecraft.world.item.component.SwingAnimation animation,
      boolean bl,
      CallbackInfoReturnable<Boolean> cir) {
    LivingEntity self = (LivingEntity) (Object) this;
    if (self instanceof net.minecraft.client.player.LocalPlayer
        && !me.friendly.exeter.module.impl.toggle.misc.Swing.allowReset()
        && self.isSwinging()) {
      cir.setReturnValue(false);
    }
  }
}
