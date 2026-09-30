package me.friendly.exeter.beta.mixin;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.module.impl.toggle.render.hud.HudRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the HUD overlay after vanilla HUD. */
@Mixin(InGameHud.class)
public class MixinInGameHud {

  @Inject(method = "render(FIII)V", at = @At("RETURN"))
  private void onRender(float partialTicks, boolean flag, int width, int height, CallbackInfo info) {
    if (Exeter.getInstance() == null) {
      return;
    }
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("hudrenderer");
    if (module != null) {
      HudRenderer.renderAll(width, height);
    }
  }
}
