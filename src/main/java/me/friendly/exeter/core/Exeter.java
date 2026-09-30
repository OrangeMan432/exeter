package me.friendly.exeter.core;

import java.io.File;
import me.friendly.api.event.basic.BasicEventManager;
import me.friendly.exeter.config.ConfigManager;
import me.friendly.exeter.config.ExeterConfig;
import me.friendly.exeter.friend.FriendManager;
import me.friendly.exeter.keybind.KeybindManager;
import me.friendly.exeter.logging.Logger;
import me.friendly.exeter.module.ModuleManager;

/**
 * Exeter client for Beta 1.7.3.
 *
 * <p>Beta port of the Exeter client, rebuilt against the Babric toolchain. Module waves register
 * into the managers below; framework pieces mirror the modern client where the beta API allows.
 */
public final class Exeter {
  private static Exeter instance = null;
  public static final String TITLE = "Exeter";
  private BasicEventManager eventManager;
  private KeybindManager keybindManager;
  private ModuleManager moduleManager;
  private FriendManager friendManager;
  private ConfigManager configManager;
  private ExeterConfig exeterConfig;
  private File directory;

  public Exeter() {
    Logger.getLogger().print("Initializing...");
    instance = this;

    this.directory = new File("exeter");
    if (!this.directory.exists()) {
      Logger.getLogger()
          .print(
              String.format(
                  "%s client directory.", this.directory.mkdir() ? "Created" : "Failed to create"));
    }

    this.eventManager = new BasicEventManager();
    this.configManager = new ConfigManager();
    this.exeterConfig = new ExeterConfig();
    this.friendManager = new FriendManager();
    this.keybindManager = new KeybindManager();
    this.moduleManager = new ModuleManager();

    this.getConfigManager().getRegistry().forEach(config -> config.load(new Object[0]));
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread("Shutdown Hook Thread") {
              @Override
              public void run() {
                Logger.getLogger().print("Shutting down...");
                getConfigManager().getRegistry().forEach(config -> config.save(new Object[0]));
                Logger.getLogger().print("Shutdown.");
              }
            });
    Logger.getLogger().print("Initialized.");
  }

  public static Exeter getInstance() {
    return instance;
  }

  public ModuleManager getModuleManager() {
    return this.moduleManager;
  }

  public KeybindManager getKeybindManager() {
    return this.keybindManager;
  }

  public FriendManager getFriendManager() {
    return this.friendManager;
  }

  public BasicEventManager getEventManager() {
    return this.eventManager;
  }

  public ConfigManager getConfigManager() {
    return this.configManager;
  }

  public ExeterConfig getExeterConfig() {
    return this.exeterConfig;
  }

  public File getDirectory() {
    return this.directory;
  }
}
