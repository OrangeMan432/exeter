package me.larp.api.minecraft.render.font;

import java.awt.*;
import org.lwjgl.opengl.GL11;

public class CustomFontRenderer extends CustomFont {
  protected CharData[] boldChars = new CharData[256];
  protected CharData[] italicChars = new CharData[256];
  protected CharData[] boldItalicChars = new CharData[256];

  private final int[] colorCode = new int[32];

  public CustomFontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
    super(font, antiAlias, fractionalMetrics);
    setupMinecraftColorcodes();
    setupBoldItalicIDs();
  }

  public void drawString(String text, float x, float y, int color) {
    drawString(text, x, y, color, false);
  }

  public void drawStringWithShadow(String text, double x, double y, int color) {
    drawString(text, x + 1D, y + 1D, color, true);
    drawString(text, x, y, color, false);
  }

  public void drawCenteredStringWithShadow(String text, float x, float y, int color) {
    drawStringWithShadow(text, x - getStringWidth(text) / 2f, y, color);
  }

  public void drawCenteredString(String text, float x, float y, int color) {
    drawString(text, x - getStringWidth(text) / 2f, y, color);
  }

  public void drawString(String text, double x, double y, int c, boolean shadow) {
    int color = c;

    x -= 1;
    y -= 2;
    if (text == null) return;
    if (color == 553648127) color = 16777215;
    if ((color & 0xFC000000) == 0) color |= -16777216;

    if (shadow) color = (color & 0xFCFCFC) >> 2 | color & 0xFF000000;

    CharData[] currentData = this.charData;
    float alpha = (color >> 24 & 0xFF) / 255.0F;
    boolean bold = false;
    boolean italic = false;
    boolean strikethrough = false;
    boolean underline = false;
    x *= 2.0D;
    y *= 2.0D;
    GL11.glPushMatrix();
    GL11.glScaled(0.5D, 0.5D, 0.5D);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(
        (color >> 16 & 0xFF) / 255.0F,
        (color >> 8 & 0xFF) / 255.0F,
        (color & 0xFF) / 255.0F,
        alpha);
    int size = text.length();
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.texId);
    for (int i = 0; i < size; i++) {
      char character = text.charAt(i);
      if (character == '\u00A7') {
        int colorIndex = 21;
        try {
          colorIndex = "0123456789abcdefklmnor".indexOf(text.charAt(i + 1));
        } catch (Exception ignored) {
        }
        if (colorIndex < 16) {
          bold = false;
          italic = false;
          underline = false;
          strikethrough = false;
          GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.texId);
          currentData = this.charData;
          if (colorIndex < 0) colorIndex = 15;
          if (shadow) colorIndex += 16;
          int cCode = this.colorCode[colorIndex];
          GL11.glColor4f(
              (cCode >> 16 & 0xFF) / 255.0F,
              (cCode >> 8 & 0xFF) / 255.0F,
              (cCode & 0xFF) / 255.0F,
              alpha);
        } else if (colorIndex == 17) {
          bold = true;
          if (italic) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texItalicBoldId);
            currentData = this.boldItalicChars;
          } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texBoldId);
            currentData = this.boldChars;
          }
        } else if (colorIndex == 18) strikethrough = true;
        else if (colorIndex == 19) underline = true;
        else if (colorIndex == 20) {
          italic = true;
          if (bold) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texItalicBoldId);
            currentData = this.boldItalicChars;
          } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texItalicId);
            currentData = this.italicChars;
          }
        } else if (colorIndex == 21) {
          bold = false;
          italic = false;
          underline = false;
          strikethrough = false;
          GL11.glColor4f(
              (color >> 16 & 0xFF) / 255.0F,
              (color >> 8 & 0xFF) / 255.0F,
              (color & 0xFF) / 255.0F,
              alpha);
          GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.texId);
          currentData = this.charData;
        }
        i++;
      } else if (character < currentData.length) {
        GL11.glBegin(GL11.GL_TRIANGLES);
        drawChar(currentData, character, (float) x, (float) y);
        GL11.glEnd();
        if (strikethrough)
          drawLine(
              x,
              y + currentData[character].height / 2f,
              x + currentData[character].width - 8.0D,
              y + currentData[character].height / 2f);
        if (underline)
          drawLine(
              x,
              y + currentData[character].height - 2.0D,
              x + currentData[character].width - 8.0D,
              y + currentData[character].height - 2.0D);
        x += currentData[character].width - 8 + this.charOffset;
      }
    }
    GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
    GL11.glPopMatrix();
  }

  @Override
  public int getStringWidth(String text) {
    if (text == null) return 0;

    int width = 0;
    CharData[] currentData = this.charData;
    int size = text.length();

    for (int i = 0; i < size; i++) {
      char character = text.charAt(i);
      if (character == '\u00A7') i++;
      else if (character < currentData.length)
        width += currentData[character].width - 8 + this.charOffset;
    }

    return width / 2;
  }

  public void setFont(Font font) {
    super.setFont(font);
    setupBoldItalicIDs();
  }

  public void setAntiAlias(boolean antiAlias) {
    super.setAntiAlias(antiAlias);
    setupBoldItalicIDs();
  }

  public void setFractionalMetrics(boolean fractionalMetrics) {
    super.setFractionalMetrics(fractionalMetrics);
    setupBoldItalicIDs();
  }

  protected int texBoldId = -1;
  protected int texItalicId = -1;
  protected int texItalicBoldId = -1;

  private void setupBoldItalicIDs() {
    texBoldId =
        setupTexture(
            this.font.deriveFont(Font.BOLD),
            this.antiAlias,
            this.fractionalMetrics,
            this.boldChars);
    texItalicId =
        setupTexture(
            this.font.deriveFont(Font.ITALIC),
            this.antiAlias,
            this.fractionalMetrics,
            this.italicChars);
    texItalicBoldId =
        setupTexture(
            this.font.deriveFont(Font.BOLD | Font.ITALIC),
            this.antiAlias,
            this.fractionalMetrics,
            this.boldItalicChars);
  }

  private void drawLine(double x, double y, double x1, double y1) {
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glLineWidth((float) 1.0);
    GL11.glBegin(1);
    GL11.glVertex2d(x, y);
    GL11.glVertex2d(x1, y1);
    GL11.glEnd();
    GL11.glEnable(GL11.GL_TEXTURE_2D);
  }

  private void setupMinecraftColorcodes() {
    for (int index = 0; index < 32; index++) {
      int noClue = (index >> 3 & 0x1) * 85;
      int red = (index >> 2 & 0x1) * 170 + noClue;
      int green = (index >> 1 & 0x1) * 170 + noClue;
      int blue = (index & 0x1) * 170 + noClue;

      if (index == 6) red += 85;

      if (index >= 16) {
        red /= 4;
        green /= 4;
        blue /= 4;
      }

      this.colorCode[index] = ((red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF);
    }
  }
}
