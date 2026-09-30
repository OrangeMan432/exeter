package me.friendly.exeter.beta.mixin;

import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dispatches client tick events around the game tick. */
@Mixin(Minecraft.class)
public class MixinMinecraftTick {

  @Inject(method = "tick()V", at = @At("HEAD"))
  private void onTickStart(CallbackInfo info) {
    if (Exeter.getInstance() != null) {
      Exeter.getInstance().getEventManager().dispatch(new TickEvent(Stage.PRE));
    }
  }

  @Inject(method = "tick()V", at = @At("RETURN"))
  private void onTickEnd(CallbackInfo info) {
    if (Exeter.getInstance() != null) {
      Exeter.getInstance().getEventManager().dispatch(new TickEvent(Stage.POST));
    }
  }
}
