package me.friendly.exeter.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Nicotine-ported 3D box rendering, adapted to 26.2 vertex signatures
 * (no entry params). Outline-only via vanilla lines pipeline.
 */
public final class Render3D {

  private Render3D() {}

  public static void drawBoxOutline(
      SubmitNodeStorage storage,
      Camera camera,
      PoseStack matrices,
      AABB box,
      int color,
      float lineWidth) {
    Vec3 cam = camera.position();
    double x0 = box.minX - cam.x;
    double y0 = box.minY - cam.y;
    double z0 = box.minZ - cam.z;
    double x1 = box.maxX - cam.x;
    double y1 = box.maxY - cam.y;
    double z1 = box.maxZ - cam.z;
    storage.submitCustomGeometry(
        matrices,
        RenderTypes.lines(),
        (pose, buffer) -> {
          // Bottom square.
          line(buffer, x0, y0, z0, x1, y0, z0, color, lineWidth);
          line(buffer, x1, y0, z0, x1, y0, z1, color, lineWidth);
          line(buffer, x1, y0, z1, x0, y0, z1, color, lineWidth);
          line(buffer, x0, y0, z1, x0, y0, z0, color, lineWidth);
          // Top square.
          line(buffer, x0, y1, z0, x1, y1, z0, color, lineWidth);
          line(buffer, x1, y1, z0, x1, y1, z1, color, lineWidth);
          line(buffer, x1, y1, z1, x0, y1, z1, color, lineWidth);
          line(buffer, x0, y1, z1, x0, y1, z0, color, lineWidth);
          // Verticals.
          line(buffer, x0, y0, z0, x0, y1, z0, color, lineWidth);
          line(buffer, x1, y0, z0, x1, y1, z0, color, lineWidth);
          line(buffer, x1, y0, z1, x1, y1, z1, color, lineWidth);
          line(buffer, x0, y0, z1, x0, y1, z1, color, lineWidth);
        });
  }

  private static void line(      com.mojang.blaze3d.vertex.VertexConsumer buffer,
      double x0,
      double y0,
      double z0,
      double x1,
      double y1,
      double z1,
      int color,
      float lineWidth) {
    float dx = (float) (x1 - x0);
    float dy = (float) (y1 - y0);
    float dz = (float) (z1 - z0);
    float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    float nx = len == 0 ? 1f : dx / len;
    float ny = len == 0 ? 0f : dy / len;
    float nz = len == 0 ? 0f : dz / len;
    buffer
        .addVertex((float) x0, (float) y0, (float) z0)
        .setColor(color)
        .setNormal(nx, ny, nz)
        .setLineWidth(lineWidth);
    buffer
        .addVertex((float) x1, (float) y1, (float) z1)
        .setColor(color)
        .setNormal(nx, ny, nz)
        .setLineWidth(lineWidth);
  }

  /** Nicotine-pattern tracer from crosshair to the target position. */
  public static void drawTracer(
      SubmitNodeStorage storage,
      Camera camera,
      PoseStack matrices,
      Vec3 from,
      Vec3 targetPos,
      int color,
      float lineWidth) {
    Vec3 adjusted = targetPos.subtract(camera.position());
    Vec3 origin = from.subtract(camera.position());
    float dx = (float) (adjusted.x - origin.x);
    float dy = (float) (adjusted.y - origin.y);
    float dz = (float) (adjusted.z - origin.z);
    float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (len < 0.001f) return;
    float nx = dx / len;
    float ny = dy / len;
    float nz = dz / len;
    storage.submitCustomGeometry(
        matrices,
        RenderTypes.lines(),
        (pose, buffer) -> {
          buffer
              .addVertex((float) origin.x, (float) origin.y, (float) origin.z)
              .setColor(color)
              .setNormal(nx, ny, nz)
              .setLineWidth(lineWidth);
          buffer
              .addVertex((float) adjusted.x, (float) adjusted.y, (float) adjusted.z)
              .setColor(color)
              .setNormal(nx, ny, nz)
              .setLineWidth(lineWidth);
        });
  }

  /** Nicotine-pattern floating text at a world position. */
  public static void drawText(
      SubmitNodeStorage storage,
      PoseStack matrices,
      Camera camera,
      net.minecraft.client.Minecraft mc,
      Vec3 position,
      String text,
      int color,
      float scale) {
    Font font = mc.font;
    Vec3 cameraPos = camera.position();
    matrices.pushPose();
    matrices.translate(
        (float) (position.x - cameraPos.x),
        (float) (position.y - cameraPos.y) + 0.5f,
        (float) (position.z - cameraPos.z));
    matrices.mulPose(camera.rotation());
    float size = 0.025f * scale;
    matrices.scale(size, -size, size);
    float x = (float) font.width(text) / 2.0f;
    storage.submitText(
        matrices,
        -x,
        0,
        Component.literal(text).getVisualOrderText(),
        false,
        Font.DisplayMode.SEE_THROUGH,
        0,
        color,
        0x50000000,
        0);
    matrices.popPose();
  }
}
