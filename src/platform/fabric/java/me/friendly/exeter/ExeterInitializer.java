package me.friendly.exeter;

import java.io.File;
import me.friendly.exeter.platform.ExeterBootstrap;
import me.friendly.exeter.platform.ExeterStorageProvider;
import me.friendly.exeter.platform.FileExeterStorage;
import me.friendly.exeter.test.SmokeTest;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

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

    // The smoke harness needs a real filesystem and a background thread, so it lives here rather
    // than in core.
    ExeterBootstrap.setPostInitHook(SmokeTest::maybeStart);

    // A JVM has a shutdown event; a browser tab does not.
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  ExeterBootstrap.saveAll();
                  System.out.println("[Exeter] Shutdown.");
                },
                "Exeter-Shutdown"));

    ExeterBootstrap.init();
  }
}
