package me.friendly.exeter.mixin;

import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.impl.toggle.movement.Sprint;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LocalPlayer.class)
public class MixinClientPlayer {

  @Inject(method = "tick", at = @At("HEAD"))
  public void onTick(CallbackInfo info) {
    Exeter.getInstance().getEventManager().dispatch(new TickEvent(Stage.PRE));
  }

  @Inject(method = "tick", at = @At("RETURN"))
  public void onTickReturn(CallbackInfo info) {
    Exeter.getInstance().getEventManager().dispatch(new TickEvent(Stage.POST));
  }

  @Inject(method = "shouldStopRunSprinting", at = @At("HEAD"), cancellable = true)
  private void onShouldStopRunSprinting(CallbackInfoReturnable<Boolean> cir) {
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("sprint");
    if (module instanceof Sprint sprint && sprint.isRunning() && sprint.isOmni()) {
      LocalPlayer player = (LocalPlayer) (Object) this;
      if (player.input.hasForwardImpulse()
          || player.input.keyPresses.left()
          || player.input.keyPresses.right()
          || player.input.keyPresses.backward()) {
        cir.setReturnValue(false);
      }
    }
  }
}
