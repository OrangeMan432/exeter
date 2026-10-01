package me.friendly.api.minecraft.render.font;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.platform.TextureUtil;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.GlyphStitcher;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FreeType;

/**
 * Builds vanilla {@link Font} instances from TrueType data using the game's own freetype
 * pipeline. Bundled fonts load through the resource manager; system fonts load from file
 * bytes with the same provider construction vanilla uses.
 */
public final class TtfFont {
  private TtfFont() {}

  public static Font fromResource(Identifier location, float size, float oversample) {
    TrueTypeGlyphProviderDefinition definition =
        new TrueTypeGlyphProviderDefinition(
            location,
            size,
            oversample,
            TrueTypeGlyphProviderDefinition.Shift.NONE,
            "");
    GlyphProviderDefinition.Loader loader =
        definition
            .unpack()
            .left()
            .orElseThrow(() -> new IllegalStateException("No TTF loader for " + location));
    ResourceManager resources = Minecraft.getInstance().getResourceManager();
    GlyphProvider provider;
    try {
      provider = loader.load(resources);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load TTF " + location, e);
    }
    return build(provider, location);
  }

  public static Font fromFile(java.io.File file, float size, float oversample) {
    ByteBuffer buffer;
    try (InputStream in = new FileInputStream(file)) {
      buffer = TextureUtil.readResource(in);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read font file " + file, e);
    }
    GlyphProvider provider;
    synchronized (net.minecraft.client.gui.font.providers.FreeTypeUtil.LIBRARY_LOCK) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
        org.lwjgl.PointerBuffer pointer = stack.mallocPointer(1);
        net.minecraft.client.gui.font.providers.FreeTypeUtil.assertError(
            FreeType.FT_New_Memory_Face(
                net.minecraft.client.gui.font.providers.FreeTypeUtil.getLibrary(),
                buffer,
                0L,
                pointer),
            "Initializing font face");
        FT_Face face = FT_Face.create(pointer.get(0));
        net.minecraft.client.gui.font.providers.FreeTypeUtil.assertError(
            FreeType.FT_Select_Charmap(face, FreeType.FT_ENCODING_UNICODE),
            "Find unicode charmap");
        provider =
            new com.mojang.blaze3d.font.TrueTypeGlyphProvider(
                buffer, face, size, oversample, 0.0F, 0.0F, "");
      }
    }
    return build(provider, Identifier.fromNamespaceAndPath("exeter", "fonts/system"));
  }

  private static Font build(GlyphProvider provider, Identifier stitchId) {
    Minecraft mc = Minecraft.getInstance();
    TextureManager textures = mc.getTextureManager();
    FontSet set = new FontSet(new GlyphStitcher(textures, stitchId));
    set.reload(
        List.of(
            new GlyphProvider.Conditional(
                provider, net.minecraft.client.gui.font.FontOption.Filter.ALWAYS_PASS)),
        Set.<FontOption>of());
    return new Font(
        new Font.Provider() {
          @Override
          public GlyphSource glyphs(FontDescription description) {
            return set.source(false);
          }

          @Override
          public EffectGlyph effect() {
            return set.whiteGlyph();
          }
        });
  }
}
