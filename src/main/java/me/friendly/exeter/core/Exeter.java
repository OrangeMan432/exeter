package me.friendly.exeter.core;

import me.friendly.api.event.basic.BasicEventManager;
import me.friendly.exeter.BuildInfo;
import me.friendly.exeter.account.AccountManager;
import me.friendly.exeter.command.CommandManager;
import me.friendly.exeter.config.ConfigManager;
import me.friendly.exeter.config.ExeterConfig;
import me.friendly.exeter.friend.FriendManager;
import me.friendly.exeter.keybind.KeybindManager;
import me.friendly.exeter.logging.Logger;
import me.friendly.exeter.module.ModuleManager;
import me.friendly.exeter.platform.ExeterBootstrap;

/**
 * Exeter client for Fabric 26.2
 *
 * <p>Exeter client. A client created by Friendly, for Minecraft version 1.8. It has been released
 * or leaked on that version. Gopro336 has obtained that version, and here, has reconstructed the
 * original source code. In this process, Gopro has also ported the client to his preferred version
 * and platform, Minecraft 26.2 Fabric. Furthermore, Gopro has done work to clean up the decompiled
 * code, and javadoc it.
 *
 * @author Friendly
 * @author Gopro336
 * @version b24
 */
public final class Exeter {
  private static Exeter instance = null;
  public static final String TITLE = "Exeter";
  public static final String HASH = BuildInfo.HASH;
  public static final String BUILD = BuildInfo.BUILD;
  public static final boolean DIRTY = BuildInfo.DIRTY;
  public final long startTime = System.nanoTime() / 1000000L;
  private BasicEventManager eventManager;
  private KeybindManager keybindManager;
  private ModuleManager moduleManager;
  private CommandManager commandManager;
  private FriendManager friendManager;
  private ConfigManager configManager;
  private ExeterConfig exeterConfig;
  private AccountManager accountManager;

  public Exeter() {

    Logger.getLogger().print("Initializing...");
    instance = this;

    this.eventManager = new BasicEventManager();
    this.configManager = new ConfigManager();
    this.friendManager = new FriendManager();
    this.keybindManager = new KeybindManager();
    this.commandManager = new CommandManager();
    this.exeterConfig = new ExeterConfig();
    this.moduleManager = new ModuleManager();
    this.accountManager = new AccountManager();
    me.friendly.exeter.util.TotemPopTracker.getInstance();
    this.getConfigManager().getRegistry().forEach(config -> config.load(new Object[0]));
    Logger.getLogger()
        .print(
            String.format(
                "Initialized, took %s milliseconds.",
                System.nanoTime() / 1000000L - this.startTime));
    ExeterBootstrap.runPostInit();
  }

  /**
   * Writes every registered config back to storage.
   *
   * <p>Hosts must call this when the game is going away. A desktop JVM does it from a shutdown
   * hook, but a browser tab has no JVM shutdown event, so an Eaglercraft build calls it when the
   * window closes or the player leaves the world. Without it, settings only persist whenever
   * something else happens to save them, such as closing the ClickGUI.
   */
  public void saveAll() {
    Logger.getLogger().print("Saving config...");
    this.getConfigManager().getRegistry().forEach(config -> config.save(new Object[0]));
  }

  public static Exeter getInstance() {
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

  public ExeterConfig getExeterConfig() {
    return this.exeterConfig;
  }

  public AccountManager getAccountManager() {
    return this.accountManager;
  }
}
