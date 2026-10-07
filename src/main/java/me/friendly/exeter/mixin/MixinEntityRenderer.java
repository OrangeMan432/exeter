package me.friendly.exeter.mixin;

import me.friendly.exeter.module.impl.toggle.render.Nametags;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips vanilla nameplate extraction for entities our Nametags module draws, so the two tags never
 * double up. Hooks the final funnel method because every living-entity and player renderer
 * overrides the virtual one.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

  @Inject(
      method =
          "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
      at = @At("HEAD"),
      cancellable = true)
  private void suppressTaggedName(
      Entity entity,
      EntityRenderState state,
      float partialTick,
      double maxDistance,
      double yOffset,
      CallbackInfo info) {
    if (Nametags.shouldTag(entity)) {
      info.cancel();
    }
  }
}
