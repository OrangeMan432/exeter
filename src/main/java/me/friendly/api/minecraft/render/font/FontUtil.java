package me.friendly.api.minecraft.render.font;

import me.friendly.api.minecraft.render.RenderMethods;
import net.minecraft.client.Minecraft;

public class FontUtil {

  public static void drawString(String text, float x, float y, int color) {
    if (RenderMethods.guiGraphics != null) {
      RenderMethods.guiGraphics.text(
          Minecraft.getInstance().font, text, (int) x, (int) y, color, true);
    }
  }

  public static int getStringWidth(String text) {
    return Minecraft.getInstance().font.width(text);
  }
}
