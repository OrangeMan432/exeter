package me.larp.client.mixin;

import me.larp.client.core.Larp;
import me.larp.client.events.InputEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

  @Inject(method = "keyPress", at = @At("HEAD"))
  private void onKeyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
    if (action == 1 && Larp.getInstance() != null) {
      Larp.getInstance()
          .getEventManager()
          .dispatch(new InputEvent(InputEvent.Type.KEYBOARD_KEY_PRESS, event.key()));
    }
  }
}
