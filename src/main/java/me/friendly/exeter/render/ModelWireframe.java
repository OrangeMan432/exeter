package me.friendly.exeter.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Captures the exact faces vanilla renders for an entity by submitting it to its own renderer with
 * an intercepted collector. Pixel-perfect by construction: body yaw, avatar scale, crouch, death,
 * layers and custom geometry all match, for every entity type. Faces come back as world-space
 * quads.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ModelWireframe {
  public record Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {}

  private static final Set<String> SECOND_LAYER =
      Set.of("hat", "jacket", "left_sleeve", "right_sleeve", "left_pants", "right_pants");

  private ModelWireframe() {}

  public static float currentPartialTick() {
    return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
  }

  public static List<Quad> capture(Entity entity, float partialTick) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return List.of();
    float tickDelta = mc.level.tickRateManager().isFrozen() ? 1f : partialTick;

    EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
    EntityRenderState state = (EntityRenderState) renderer.createRenderState(entity, tickDelta);

    double ox = Mth.lerp(tickDelta, entity.xOld, entity.getX());
    double oy = Mth.lerp(tickDelta, entity.yOld, entity.getY());
    double oz = Mth.lerp(tickDelta, entity.zOld, entity.getZ());
    Vec3 renderOffset = renderer.getRenderOffset(state);

    Capture capture = new Capture();
    PoseStack pose = new PoseStack();
    CameraRenderState camera = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
    renderer.submit(state, pose, capture, camera);

    List<Quad> quads = new ArrayList<>(capture.quads.size());
    for (Quad q : capture.quads) {
      quads.add(
          new Quad(
              offset(q.a(), ox, oy, oz, renderOffset),
              offset(q.b(), ox, oy, oz, renderOffset),
              offset(q.c(), ox, oy, oz, renderOffset),
              offset(q.d(), ox, oy, oz, renderOffset)));
    }
    return quads;
  }

  private static Vec3 offset(Vec3 v, double ox, double oy, double oz, Vec3 renderOffset) {
    return new Vec3(
        v.x + ox + renderOffset.x, v.y + oy + renderOffset.y, v.z + oz + renderOffset.z);
  }

  /** Intercepts submitted models and records every emitted face as a quad. */
  private static final class Capture extends SubmitNodeStorage {
    final List<Quad> quads = new ArrayList<>();
    final FaceConsumer consumer = new FaceConsumer();

    Capture() {
      this.consumer.quads = quads;
    }

    @Override
    public <S> void submitModel(
        Model<? super S> model,
        S state,
        PoseStack poseStack,
        RenderType renderType,
        int lightCoords,
        int overlayCoords,
        int tintedColor,
        UvMapping uvMapping,
        int outlineColor) {
      if (renderType.isOutline()) return;
      model.setupAnim(state);
      // Hide after setupAnim: PlayerModel.setupAnim drives part visibility
      // from skin settings and would undo an earlier hide.
      List<ModelPart> hidden = hideSecondLayer(model);
      try {
        model.renderToBuffer(poseStack, consumer, lightCoords, overlayCoords, tintedColor);
      } finally {
        for (ModelPart part : hidden) part.visible = true;
      }
    }

    @Override
    public void submitCustomGeometry(
        PoseStack poseStack,
        RenderType renderType,
        SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
      if (renderType.isOutline()) return;
      customGeometryRenderer.render(poseStack.last(), consumer);
    }

    private static final java.util.Map<String, String> LAYER_PARENTS =
        java.util.Map.of(
            "hat", "head",
            "jacket", "body",
            "left_sleeve", "left_arm",
            "right_sleeve", "right_arm",
            "left_pants", "left_leg",
            "right_pants", "right_leg");

    private List<ModelPart> hideSecondLayer(Model<?> model) {
      List<ModelPart> hidden = new ArrayList<>(6);
      ModelPart root;
      try {
        root = model.root();
      } catch (Exception e) {
        return hidden;
      }
      for (String name : SECOND_LAYER) {
        ModelPart part = findPart(root, name);
        if (part != null && part.visible) {
          part.visible = false;
          hidden.add(part);
        }
      }
      return hidden;
    }

    private ModelPart findPart(ModelPart root, String name) {
      if (root.hasChild(name)) return root.getChild(name);
      String parent = LAYER_PARENTS.get(name);
      if (parent != null && root.hasChild(parent)) {
        ModelPart parentPart = root.getChild(parent);
        if (parentPart.hasChild(name)) return parentPart.getChild(name);
      }
      return null;
    }
  }

  /** Groups every 4 emitted vertices into one face quad. */
  private static final class FaceConsumer implements VertexConsumer {
    List<Quad> quads;
    final float[] xs = new float[4];
    final float[] ys = new float[4];
    final float[] zs = new float[4];
    int i;

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
      xs[i] = x;
      ys[i] = y;
      zs[i] = z;
      if (++i == 4) {
        quads.add(
            new Quad(
                new Vec3(xs[0], ys[0], zs[0]),
                new Vec3(xs[1], ys[1], zs[1]),
                new Vec3(xs[2], ys[2], zs[2]),
                new Vec3(xs[3], ys[3], zs[3])));
        i = 0;
      }
      return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
      return this;
    }

    @Override
    public VertexConsumer setColor(int argb) {
      return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
      return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
      return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
      return this;
    }

    @Override
    public VertexConsumer setUv3(float u, float v) {
      return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
      return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
      return this;
    }
  }
}
