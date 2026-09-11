package me.larp.client.mixin;

import me.larp.api.minecraft.render.RenderMethods;
import me.larp.client.core.Larp;
import me.larp.client.events.RenderGameOverlayEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class MixinGuiIngame {

  @Inject(method = "extractRenderState", at = @At("HEAD"))
  private void onExtractRenderState(
      GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
    RenderMethods.guiGraphics = guiGraphics;
    Larp.getInstance()
        .getEventManager()
        .dispatch(new RenderGameOverlayEvent(RenderGameOverlayEvent.Type.IN_GAME));
  }
}
