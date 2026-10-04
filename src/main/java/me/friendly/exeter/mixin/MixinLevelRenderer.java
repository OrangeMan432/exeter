package me.friendly.exeter.mixin;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.WorldRenderEvent;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.Gizmos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

  @Inject(method = "collectPerFrameRenderThreadGizmos", at = @At("RETURN"))
  private void onCollectGizmos(CallbackInfoReturnable<Gizmos.TemporaryCollection> cir) {
    Exeter.getInstance().getEventManager().dispatch(new WorldRenderEvent());
  }
}
