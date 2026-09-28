package me.friendly.exeter.mixin;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.toggle.render.FreeLook;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes vanilla mouse look into FreeLook's detached camera angles when active. */
@Mixin(MouseHandler.class)
public class MixinMouseHandler {

  private static FreeLook freeLook() {
    if (Exeter.getInstance() == null) {
      return null;
    }
    var module = Exeter.getInstance().getModuleManager().getModuleByAlias("freelook");
    if (module instanceof FreeLook freeLook && freeLook.isFreeLooking()) {
      return freeLook;
    }
    return null;
  }

  @Inject(method = "turnPlayer(D)V", at = @At("HEAD"))
  private void preTurn(double delta, CallbackInfo info) {
    FreeLook freeLook = freeLook();
    if (freeLook != null) {
      freeLook.recordPlayerRotation();
    }
  }

  @Inject(method = "turnPlayer(D)V", at = @At("TAIL"))
  private void postTurn(double delta, CallbackInfo info) {
    FreeLook freeLook = freeLook();
    if (freeLook != null) {
      freeLook.redirectTurn();
    }
  }
}
