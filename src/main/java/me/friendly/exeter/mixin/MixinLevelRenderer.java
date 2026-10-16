package me.friendly.exeter.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.WorldRenderEvent;
import me.friendly.exeter.render.ModelWireframe;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.gizmos.Gizmos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

  @Inject(method = "collectPerFrameRenderThreadGizmos", at = @At("RETURN"))
  private void onCollectGizmos(CallbackInfoReturnable<Gizmos.TemporaryCollection> cir) {
    Exeter.getInstance().getEventManager().dispatch(new WorldRenderEvent());
  }

  @Inject(method = "submitEntities", at = @At("TAIL"))
  private void exeter$submitChamsGlint(
      PoseStack poseStack,
      LevelRenderState levelRenderState,
      SubmitNodeCollector collector,
      CallbackInfo ci) {
    ModelWireframe.submitGlint(poseStack, collector, levelRenderState);
  }
}
