package me.friendly.api.minecraft.render;

import org.lwjgl.opengl.GL11;

/** Immediate-mode GL shapes for ClickGUI icons (arrows, gears). */
public final class GlShapes {
  private GlShapes() {}

  private static void setup(int color) {
    float a = (float) (color >> 24 & 0xFF) / 255.0f;
    float r = (float) (color >> 16 & 0xFF) / 255.0f;
    float g = (float) (color >> 8 & 0xFF) / 255.0f;
    float b = (float) (color & 0xFF) / 255.0f;
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(r, g, b, a);
  }

  private static void restore() {
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
  }

  private static float[] rotate(float x, float y, float angleDeg) {
    double rad = Math.toRadians(angleDeg);
    double cos = Math.cos(rad);
    double sin = Math.sin(rad);
    return new float[] {(float) (x * cos - y * sin), (float) (x * sin + y * cos)};
  }

  /** Filled triangle pointing down at 0 degrees, rotated clockwise. */
  public static void drawTriangle(float cx, float cy, float size, float angleDeg, int color) {
    float[][] base = new float[][] {{0.0f, size}, {-size * 0.9f, -size * 0.7f}, {size * 0.9f, -size * 0.7f}};
    setup(color);
    GL11.glBegin(GL11.GL_TRIANGLES);
    for (int i = 0; i < 3; i++) {
      float[] p = rotate(base[i][0], base[i][1], angleDeg);
      GL11.glVertex2f(cx + p[0], cy + p[1]);
    }
    GL11.glEnd();
    restore();
  }

  /** Hollow ring, used as the settings gear. */
  public static void drawRing(float cx, float cy, float radius, int color) {
    setup(color);
    GL11.glLineWidth(1.5f);
    GL11.glBegin(GL11.GL_LINE_LOOP);
    int segments = 16;
    for (int i = 0; i < segments; i++) {
      double a = 2.0 * Math.PI * i / segments;
      GL11.glVertex2f(cx + (float) (Math.cos(a) * radius), cy + (float) (Math.sin(a) * radius));
    }
    GL11.glEnd();
    restore();
  }
}
