package me.friendly.exeter.beta.mixin;

import me.friendly.exeter.core.Exeter;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Singleplayer chat stub is empty in vanilla, so there is nothing to cancel here. Still
 * intercepts the prefix so commands work offline too.
 */
@Mixin(ClientPlayerEntity.class)
public class MixinChatSendSP {

  @Inject(method = "sendChatMessage(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
  private void onSendChat(String message, CallbackInfo info) {
    if (Exeter.getInstance() == null) {
      return;
    }
    if (Exeter.getInstance().getCommandManager().dispatch(message)) {
      info.cancel();
    }
  }
}
