package me.friendly.exeter.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.RenderWorldEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class)
public abstract class MixinLevelRenderer {

  @Unique private Camera exeter$camera;
  @Unique private PoseStack exeter$matrixStack;

  @Final @Shadow private SubmitNodeStorage submitNodeStorage;

  @Inject(
      method = "render",
      at = @At("HEAD"))
  private void onRenderHead(
      GraphicsResourceAllocator resourceAllocator,
      DeltaTracker deltaTracker,
      boolean renderOutline,
      CameraRenderState cameraState,
      Matrix4fc modelViewMatrix,
      GpuBufferSlice terrainFog,
      Vector4f fogColor,
      boolean shouldRenderSky,
      CallbackInfo info) {
    this.exeter$camera = Minecraft.getInstance().gameRenderer.mainCamera();
    this.exeter$matrixStack = new PoseStack();
  }

  @Inject(
      method = "lambda$addMainPass$0",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeTranslucent()V",
              shift = At.Shift.AFTER))
  private void onAfterTranslucent(CallbackInfo info) {
    if (this.exeter$camera == null || this.exeter$matrixStack == null) return;
    Exeter.getInstance()
        .getEventManager()
        .dispatch(
            new RenderWorldEvent(
                this.exeter$camera, this.exeter$matrixStack, this.submitNodeStorage));
  }
}
