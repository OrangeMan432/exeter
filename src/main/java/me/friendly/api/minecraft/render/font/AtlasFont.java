package me.friendly.api.minecraft.render.font;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Meteor-style self-owned font atlas: rasterizes a TrueType face with AWT into a single
 * texture we fully control (no vanilla stitcher involved). Glyphs bake white at a fixed
 * height and are tinted per draw.
 */
public final class AtlasFont {
  public static final int BAKE_HEIGHT = 48;
  private static final int ATLAS_SIZE = 1024;
  private static final int COLS = 16;
  private static final int CELL = ATLAS_SIZE / COLS;
  private static final int FIRST_CODEPOINT = 32;
  private static final int GLYPH_COUNT = 224;

  public final Identifier textureId;
  public final int ascent;
  public final int cellHeight;

  private final Map<Integer, Glyph> glyphs = new HashMap<Integer, Glyph>();
  private final int spaceAdvance;
  private DynamicTexture texture;

  public static final class Glyph {
    public final int u;
    public final int v;
    public final int width;
    public final int advance;
    public final int bearing;

    Glyph(int u, int v, int width, int advance, int bearing) {
      this.u = u;
      this.v = v;
      this.width = width;
      this.advance = advance;
      this.bearing = bearing;
    }
  }

  private AtlasFont(Identifier textureId, int ascent, int spaceAdvance) {
    this.textureId = textureId;
    this.ascent = ascent;
    this.cellHeight = CELL;
    this.spaceAdvance = spaceAdvance;
  }

  public Glyph glyph(int codepoint) {
    Glyph glyph = glyphs.get(Integer.valueOf(codepoint));
    if (glyph != null) {
      return glyph;
    }
    return glyphs.get(Integer.valueOf(32));
  }

  public int advance(int codepoint) {
    Glyph glyph = glyphs.get(Integer.valueOf(codepoint));
    if (glyph != null) {
      return glyph.advance;
    }
    return spaceAdvance;
  }

  public void close() {
    if (texture != null) {
      try {
        texture.close();
      } catch (Exception ignored) {
      }
      texture = null;
    }
  }

  public static AtlasFont bake(Font awt, Identifier textureId) {
    Font sized = awt.deriveFont(Font.PLAIN, (float) BAKE_HEIGHT);
    BufferedImage image =
        new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
    g.setRenderingHint(
        RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    g.setFont(sized);
    g.setColor(Color.WHITE);
    FontMetrics metrics = g.getFontMetrics();
    int ascent = metrics.getAscent();

    AtlasFont atlas = new AtlasFont(textureId, ascent, metrics.charWidth(' '));
    java.awt.font.FontRenderContext frc = g.getFontRenderContext();
    for (int i = 0; i < GLYPH_COUNT; i++) {
      int codepoint = FIRST_CODEPOINT + i;
      int col = i % COLS;
      int row = i / COLS;
      int cellX = col * CELL;
      int cellY = row * CELL;
      String s = String.valueOf((char) codepoint);
      int advance = metrics.charWidth(codepoint);
      int bearing = 0;
      if (codepoint != 32) {
        try {
          java.awt.font.GlyphVector gv = sized.createGlyphVector(frc, s);
          double inkLeft = gv.getGlyphVisualBounds(0).getBounds2D().getMinX();
          bearing = (int) Math.round(Math.max(0.0, Math.min(inkLeft, CELL / 2.0)));
        } catch (Exception ignored) {
        }
        g.drawString(s, cellX, cellY + ascent);
      }
      atlas.glyphs.put(
          Integer.valueOf(codepoint),
          new Glyph(
              cellX, cellY, Math.min(Math.max(advance, 1), CELL), Math.max(advance, 1), bearing));
    }
    g.dispose();

    NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, ATLAS_SIZE, ATLAS_SIZE, false);
    for (int y = 0; y < ATLAS_SIZE; y++) {
      for (int x = 0; x < ATLAS_SIZE; x++) {
        int argb = image.getRGB(x, y);
        int a = (argb >> 24) & 0xFF;
        nativeImage.setPixelABGR(x, y, (a << 24) | 0x00FFFFFF);
      }
    }
    DynamicTexture dynamic =
        new DynamicTexture(
            new java.util.function.Supplier<String>() {
              @Override
              public String get() {
                return textureId.toString();
              }
            },
            nativeImage);
    Minecraft.getInstance().getTextureManager().register(textureId, dynamic);
    atlas.texture = dynamic;
    return atlas;
  }

  public static Font awtFromResource(String path) throws Exception {
    InputStream in = AtlasFont.class.getResourceAsStream(path);
    if (in == null) {
      throw new IllegalStateException("Missing bundled font " + path);
    }
    try {
      return Font.createFont(Font.TRUETYPE_FONT, in);
    } finally {
      in.close();
    }
  }

  public static Font awtFromFile(File file) throws Exception {
    return Font.createFont(Font.TRUETYPE_FONT, file);
  }

  public static Font awtSystem(String family) {
    return new Font(family, Font.PLAIN, BAKE_HEIGHT);
  }
}
