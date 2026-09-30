package me.friendly.api.minecraft.render.font;

import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.font.TextRenderer;

public final class FontUtil {
  private FontUtil() {}

  private static TextRenderer font() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    return mc != null ? mc.textRenderer : null;
  }

  public static void drawString(String text, float x, float y, int color) {
    TextRenderer font = font();
    if (font != null) {
      font.drawWithShadow(text, (int) x, (int) y, color);
    }
  }

  public static int getStringWidth(String text) {
    TextRenderer font = font();
    return font != null ? font.getWidth(text) : 0;
  }

  public static int getFontHeight() {
    return 9;
  }
}
