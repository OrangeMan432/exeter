package me.friendly.exeter.mixin;

import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
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
}
