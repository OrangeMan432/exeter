package me.friendly.exeter.events;

import com.mojang.blaze3d.vertex.PoseStack;
import me.friendly.api.event.Event;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeStorage;

public class RenderWorldEvent extends Event {
  private final Camera camera;
  private final PoseStack matrixStack;
  private final SubmitNodeStorage submitNodeStorage;

  public RenderWorldEvent(
      Camera camera, PoseStack matrixStack, SubmitNodeStorage submitNodeStorage) {
    this.camera = camera;
    this.matrixStack = matrixStack;
    this.submitNodeStorage = submitNodeStorage;
  }

  public Camera getCamera() {
    return this.camera;
  }

  public PoseStack getMatrixStack() {
    return this.matrixStack;
  }

  public SubmitNodeStorage getSubmitNodeStorage() {
    return this.submitNodeStorage;
  }
}
