package me.larp.client.core;

import java.io.*;
import java.io.File;
import me.larp.api.event.basic.BasicEventManager;
import me.larp.client.command.CommandManager;
import me.larp.client.config.ConfigManager;
import me.larp.client.config.LarpConfig;
import me.larp.client.friend.FriendManager;
import me.larp.client.gui.screens.accountmanager.AccountManager;
import me.larp.client.keybind.KeybindManager;
import me.larp.client.logging.Logger;
import me.larp.client.module.ModuleManager;
import me.larp.client.plugin.PluginManager;

/**
 * Larp client for Fabric 26.2
 *
 * <p>Larp client. A client created by Friendly, for Minecraft version 1.8. It has been released
 * or leaked on that version. Gopro336 has obtained that version, and here, has reconstructed the
 * original source code. In this process, Gopro has also ported the client to his preferred version
 * and platform, Minecraft 26.2 Fabric. Furthermore, Gopro has done work to clean up the decompiled
 * code, and javadoc it.
 *
 * @author Friendly
 * @author Gopro336
 * @version b24
 */
public final class Larp {
  private static Larp instance = null;
  public static final String TITLE = "Larp Client";
  public static final String HASH = "50db86fe1a5ebee1";
  public static final String BUILD = "b25+36";
  public static final boolean DIRTY = false;
  public final long startTime = System.nanoTime() / 1000000L;
  private BasicEventManager eventManager;
  private KeybindManager keybindManager;
  private ModuleManager moduleManager;
  private CommandManager commandManager;
  private FriendManager friendManager;
  private ConfigManager configManager;
  private LarpConfig larpConfig;
  private AccountManager accountManager;
  private PluginManager pluginManager;
  private File directory;

  public Larp() {

    Logger.getLogger().print("Initializing...");
    instance = this;

    // In exeter 1.8, the config file is named clarinet for whatever reason. Ours is larp.
    this.directory = new File(System.getProperty("user.home"), "larp");
    //        this.directory = new File(System.getProperty("user.home"), "clarinet");

    if (!this.directory.exists()) {
      Logger.getLogger()
          .print(
              String.format(
                  "%s client directory.", this.directory.mkdir() ? "Created" : "Failed to create"));
    }
    this.eventManager = new BasicEventManager();
    this.configManager = new ConfigManager();
    this.friendManager = new FriendManager();
    this.keybindManager = new KeybindManager();
    this.commandManager = new CommandManager();
    this.larpConfig = new LarpConfig();
    this.moduleManager = new ModuleManager();
    //        this.accountManager = new AccountManager();
    this.pluginManager = new PluginManager();
    this.getConfigManager().getRegistry().forEach(config -> config.load(new Object[0]));
    try {
      this.pluginManager.onLoad();
      System.out.println("Plugin manager started.");
      System.out.println(this.pluginManager.getList() + "has been loaded.");
    } catch (IOException e) {
      e.printStackTrace();
    }
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
    Logger.getLogger()
        .print(
            String.format(
                "Initialized, took %s milliseconds.",
                System.nanoTime() / 1000000L - this.startTime));
  }

  public static Larp getInstance() {
    return instance;
  }

  public ModuleManager getModuleManager() {
    return this.moduleManager;
  }

  public CommandManager getCommandManager() {
    return this.commandManager;
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

  public LarpConfig getLarpConfig() {
    return this.larpConfig;
  }

  // AccountManager is not working
  public AccountManager getAccountManager() {
    return this.accountManager;
  }

  public PluginManager getPluginManager() {
    return this.pluginManager;
  }

  public File getDirectory() {
    return this.directory;
  }
}
