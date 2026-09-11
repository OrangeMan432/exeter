package me.larp.client.mixin;

import me.larp.api.event.Stage;
import me.larp.client.core.Larp;
import me.larp.client.events.MotionUpdateEvent;
import me.larp.client.events.TickEvent;
import me.larp.client.module.Module;
import me.larp.client.module.ToggleableModule;
import me.larp.client.module.impl.toggle.movement.NoSlow;
import net.minecraft.client.Minecraft;
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
    Larp.getInstance().getEventManager().dispatch(new TickEvent(Stage.PRE));
  }

  @Inject(method = "tick", at = @At("RETURN"))
  public void onTickReturn(CallbackInfo info) {
    Larp.getInstance().getEventManager().dispatch(new TickEvent(Stage.POST));
  }

  @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"))
  public void onMotionUpdateHead(CallbackInfo info) {
    dispatchMotionUpdate(Stage.PRE);
  }

  @Inject(method = "onUpdateWalkingPlayer", at = @At("RETURN"))
  public void onMotionUpdateReturn(CallbackInfo info) {
    dispatchMotionUpdate(Stage.POST);
  }

  @Inject(method = "isSlowDueToUsingItem", at = @At("HEAD"), cancellable = true)
  private void onIsSlowDueToUsingItem(CallbackInfoReturnable<Boolean> info) {
    Module module = Larp.getInstance().getModuleManager().getModule(NoSlow.class);
    if (!(module instanceof ToggleableModule toggle && toggle.isRunning())) return;
    if (module instanceof NoSlow noSlow
        && noSlow.isFoodOnly()
        && Minecraft.getInstance().player != null) {
      var held = Minecraft.getInstance().player.getUseItem();
      if (held.isEmpty()
          || held.get(net.minecraft.core.component.DataComponents.FOOD) == null) {
        return;
      }
    }
    info.setReturnValue(false);
  }

  private void dispatchMotionUpdate(Stage stage) {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player != null) {
      Larp.getInstance()
          .getEventManager()
          .dispatch(
              new MotionUpdateEvent(
                  stage,
                  player.getX(),
                  player.getY(),
                  player.getZ(),
                  player.getYRot(),
                  player.getXRot(),
                  player.onGround()));
    }
  }
}
