package me.friendly.exeter;

import java.io.File;
import me.friendly.exeter.mixin.MixinMinecraft;
import me.friendly.exeter.platform.ExeterBootstrap;
import me.friendly.exeter.platform.ExeterStorageProvider;
import me.friendly.exeter.platform.FileExeterStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

/**
 * Java Edition (Fabric) entry point.
 *
 * <p>This class is host-specific and lives outside core. It installs the platform wiring that a
 * desktop JVM can provide, then hands over to the shared {@link ExeterBootstrap}. An Eaglercraft
 * build supplies its own entry point with different wiring and never compiles this file.
 */
public class ExeterInitializer implements ModInitializer {

  @Override
  public void onInitialize() {
    File configDir = FabricLoader.getInstance().getConfigDir().resolve("exeter").toFile();
    ExeterStorageProvider.set(new FileExeterStorage(configDir));

    // Minecraft keeps its User in a final field, so swapping sessions needs the accessor mixin.
    ExeterBootstrap.setUserSwapper(
        user -> ((MixinMinecraft) (Object) Minecraft.getInstance()).exeter$setUser(user));

    ExeterBootstrap.init();
  }
}
