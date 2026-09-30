package me.friendly.api.minecraft.render;

import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.texture.TextureManager;
import org.lwjgl.opengl.GL11;

/** Mod icon textures with shape fallback when loading fails. */
public final class GuiTextures {
  private GuiTextures() {}

  private static boolean triedLoad = false;
  private static int arrowId = 0;
  private static int gearId = 0;

  private static void ensureLoaded() {
    if (triedLoad) {
      return;
    }
    triedLoad = true;
    try {
      Minecraft mc = MinecraftAccessor.getMinecraft();
      if (mc == null) {
        return;
      }
      TextureManager manager = mc.textureManager;
      if (manager == null) {
        return;
      }
      arrowId = manager.getTextureId("/assets/exeter/textures/arrow.png");
      gearId = manager.getTextureId("/assets/exeter/textures/gear.png");
    } catch (Exception ignored) {
    }
  }

  public static int arrowId() {
    ensureLoaded();
    return arrowId;
  }

  public static int gearId() {
    ensureLoaded();
    return gearId;
  }

  private static float[] rotate(float x, float y, float angleDeg) {
    double rad = Math.toRadians(angleDeg);
    double cos = Math.cos(rad);
    double sin = Math.sin(rad);
    return new float[] {(float) (x * cos - y * sin), (float) (x * sin + y * cos)};
  }

  /**
   * Draws a centered textured quad, rotated clockwise, using full-texture UVs. Falls back to a
   * triangle when the texture failed to load.
   */
  public static void drawIcon(int textureId, float cx, float cy, float size, float angleDeg) {
    if (textureId == 0) {
      GlShapes.drawTriangle(cx, cy, size * 0.4f, angleDeg, 0xFFCCCCCC);
      return;
    }
    try {
      Minecraft mc = MinecraftAccessor.getMinecraft();
      if (mc == null || mc.textureManager == null) {
        GlShapes.drawTriangle(cx, cy, size * 0.4f, angleDeg, 0xFFCCCCCC);
        return;
      }
      float h = size / 2.0f;
      float[][] corners = new float[][] {{-h, -h}, {h, -h}, {h, h}, {-h, h}};
      float[][] uvs = new float[][] {{0.0f, 0.0f}, {1.0f, 0.0f}, {1.0f, 1.0f}, {0.0f, 1.0f}};

      GL11.glEnable(GL11.GL_TEXTURE_2D);
      mc.textureManager.bindTexture(textureId);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

      Tessellator tessellator = Tessellator.INSTANCE;
      tessellator.start(7);
      tessellator.color(1.0f, 1.0f, 1.0f, 1.0f);
      for (int i = 0; i < 4; i++) {
        float[] p = rotate(corners[i][0], corners[i][1], angleDeg);
        tessellator.texture(uvs[i][0], uvs[i][1]);
        tessellator.vertex(cx + p[0], cy + p[1], 0.0);
      }
      tessellator.draw();

      GL11.glDisable(GL11.GL_BLEND);
    } catch (Exception ignored) {
    }
  }
}
