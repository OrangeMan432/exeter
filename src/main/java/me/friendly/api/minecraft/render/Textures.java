package me.friendly.api.minecraft.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TextureProvider;
import net.minecraft.client.renderer.texture.TextureProvider2d;
import net.minecraft.client.renderer.texture.TextureResources;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Registers runtime-built 2D textures with the provider based texture manager.
 *
 * <p>The manager no longer accepts ready-made resources, so each texture is described by a factory
 * that builds a fresh image every time the manager (re)loads it. Call {@link #get(Identifier)} to
 * trigger the first upload and fetch the loaded handle.
 */
public final class Textures {
  private Textures() {}

  /** Registers a texture backed by an encoded PNG, re-read on every reload. */
  public static void register(Identifier id, byte[] png) {
    register(id, () -> readPng(id, png));
  }

  /** Registers a texture backed by a factory that must return a new image on every call. */
  public static void register(Identifier id, Supplier<NativeImage> imageFactory) {
    Minecraft.getInstance()
        .getTextureManager()
        .registerProvider(id, new RuntimeTextureProvider(imageFactory));
  }

  /** Loads the texture if needed and returns its handle. */
  public static TextureResources get(Identifier id) {
    return (TextureResources) Minecraft.getInstance().getTextureManager().getTexture(id);
  }

  private static NativeImage readPng(Identifier id, byte[] png) {
    try {
      return NativeImage.read(png);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read texture " + id, e);
    }
  }

  private static final class RuntimeTextureProvider
      implements TextureProvider<TextureContents, TextureResources> {
    private final Supplier<NativeImage> imageFactory;

    private RuntimeTextureProvider(Supplier<NativeImage> imageFactory) {
      this.imageFactory = imageFactory;
    }

    @Override
    public TextureContents prepareState(ResourceManager resourceManager, Identifier id) {
      return new TextureContents(imageFactory.get(), null);
    }

    @Override
    public TextureContents missingState() {
      return TextureContents.createMissing();
    }

    @Override
    public TextureResources createTexture(TextureContents contents, Identifier id) {
      return TextureProvider2d.INSTANCE.createTexture(contents, id);
    }

    @Override
    public void dumpContents(Identifier id, Path path, TextureResources texture) {
      TextureProvider2d.INSTANCE.dumpContents(id, path, texture);
    }

    @Override
    public void releaseTexture(TextureResources texture) {
      TextureProvider2d.INSTANCE.releaseTexture(texture);
    }
  }
}
