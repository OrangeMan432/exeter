package me.friendly.exeter.mixin;

import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.MotionUpdateEvent;
import me.friendly.exeter.events.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

  @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"))
  public void onMotionUpdateHead(CallbackInfo info) {
    dispatchMotionUpdate(Stage.PRE);
  }

  @Inject(method = "onUpdateWalkingPlayer", at = @At("RETURN"))
  public void onMotionUpdateReturn(CallbackInfo info) {
    dispatchMotionUpdate(Stage.POST);
  }

  private void dispatchMotionUpdate(Stage stage) {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player != null) {
      Exeter.getInstance()
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
