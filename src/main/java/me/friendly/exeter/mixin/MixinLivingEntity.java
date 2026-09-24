package me.friendly.exeter.mixin;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TravelEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class)
public class MixinLivingEntity {

  @Inject(method = "travel", at = @At("RETURN"))
  public void onTravelReturn(Vec3 movementInput, CallbackInfo ci) {
    LivingEntity self = (LivingEntity) (Object) this;
    if (self instanceof net.minecraft.client.player.LocalPlayer) {
      Exeter.getInstance().getEventManager().dispatch(new TravelEvent());
    }
  }
}
