package me.friendly.exeter.mixin;

import me.friendly.exeter.module.impl.toggle.render.FullBright;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class MixinLightmap {
  @Inject(method = "extract", at = @At("RETURN"))
  private void forceBrightness(LightmapRenderState state, float partialTicks, CallbackInfo info) {
    if (FullBright.isGammaActive()) {
      state.brightness = 16.0F;
    }
  }
}
