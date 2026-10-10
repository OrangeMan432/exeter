package me.friendly.exeter.module.impl.toggle.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SearchSelectPopup;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SelectionPopup;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;

public class Debug extends ToggleableModule {

  private final Property<Boolean> logToFile = new Property<Boolean>(false, "Log To File");
  private final Property<Boolean> logToChat = new Property<Boolean>(false, "Log To Chat");
  private final Property<Boolean> logToNotifications =
      new Property<Boolean>(false, "Log To Notifications");
  private final Property<Boolean> showInfo = new Property<Boolean>(true, "Show Info");
  private final Property<Boolean> showWarn = new Property<Boolean>(true, "Show Warnings");
  private final Property<Boolean> showError = new Property<Boolean>(true, "Show Errors");
  private final Property<Boolean> refillTotem =
      new Property<Boolean>(false, "Refill Totem", "refilltotem");
  private final SelectionPopup.Toggles moduleToggles = new SelectionPopup.Toggles("Module Toggles");
  private final PopupProperty modulesPopup;

  private List<Module> allModules;

  public Debug() {
    super("Debug", new String[] {"debug"}, 0x00FF00, ModuleType.CLIENT);
    setDescription("Per-module debug logging to file and chat.");
    this.modulesPopup = new PopupProperty("Modules", this::openModulesPopup);
    offerProperties(
        logToFile,
        logToChat,
        logToNotifications,
        showInfo,
        showWarn,
        showError,
        refillTotem,
        moduleToggles.getProperty(),
        modulesPopup);
    logToFile.setDescription("Saves debug output to the log file.");
    logToChat.setDescription("Shows debug output in chat.");
    logToNotifications.setDescription("Shows debug output as notifications.");
    showInfo.setDescription("Includes info-level messages in debug output.");
    showWarn.setDescription("Includes warning messages in debug output.");
    showError.setDescription("Includes error messages in debug output.");
    refillTotem.setDescription("Gives you a fresh totem each time you pop one.");
    moduleToggles.getProperty().setDescription("Chooses which modules can send debug output.");
    modulesPopup.setDescription("Opens the list of modules to toggle debug output for.");
    this.listeners.add(
        new Listener<PacketEvent>("debug_totem_refill") {
          @Override
          public void call(PacketEvent event) {
            onPacket(event);
          }
        });
  }

  public void initModuleToggles(Iterable<Module> modules) {
    this.allModules = new ArrayList<>();
    for (Module module : modules) {
      if (module == this) continue;
      if (module instanceof ToggleableModule) {
        this.allModules.add(module);
        moduleToggles.getToggles().putIfAbsent(module.getLabel(), false);
      }
    }
  }

  /**
   * Picks up modules registered after startup (e.g. by plugins) so they can be toggled for debug
   * output too.
   */
  private void syncModuleToggles() {
    if (allModules == null) {
      allModules = new ArrayList<>();
    }
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      if (module == this || !(module instanceof ToggleableModule)) continue;
      if (!allModules.contains(module)) {
        allModules.add(module);
        moduleToggles.getToggles().putIfAbsent(module.getLabel(), false);
      }
    }
  }

  /** Test helper: hands the player a fresh totem via /give whenever they pop one. */
  private void onPacket(PacketEvent event) {
    if (!refillTotem.getValue()) return;
    if (!(event.getPacket() instanceof ClientboundEntityEventPacket packet)) return;
    if (packet.getEventId() != 35) return;
    if (minecraft.level == null || minecraft.player == null) return;
    if (packet.getEntity(minecraft.level) != minecraft.player) return;
    minecraft.player.connection.sendCommand("give @s minecraft:totem_of_undying");
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "totem popped, refilled via /give");
  }

  private void openModulesPopup() {
    syncModuleToggles();
    List<SearchSelectPopup.ToggleItem> items = new ArrayList<>();
    for (Module module : allModules) {
      String name = module.getLabel();
      items.add(
          SelectionPopup.toggle(
              name,
              () -> moduleToggles.getToggles().getOrDefault(name, false),
              enabled -> moduleToggles.getToggles().put(name, enabled)));
    }

    SelectionPopup.open(
        "Debug Module Toggles",
        items,
        () -> {
          moduleToggles.save();
          syncSettings();
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    DebugLogger.get().setEnabled(true);
    moduleToggles.load();
    syncSettings();
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    DebugLogger.get().setEnabled(false);
  }

  public void syncSettings() {
    DebugLogger logger = DebugLogger.get();
    logger.setEnabled(this.isRunning());
    logger.setLogToFile(logToFile.getValue());
    logger.setLogToChat(logToChat.getValue());
    logger.setLogToNotifications(logToNotifications.getValue());
    logger.setShowInfo(showInfo.getValue());
    logger.setShowWarn(showWarn.getValue());
    logger.setShowError(showError.getValue());

    for (Map.Entry<String, Boolean> entry : moduleToggles.getToggles().entrySet()) {
      logger.setModuleEnabled(entry.getKey(), entry.getValue());
    }
  }

  public Property<Boolean> getLogToFile() {
    return logToFile;
  }

  public Property<Boolean> getLogToChat() {
    return logToChat;
  }

  public Property<Boolean> getLogToNotifications() {
    return logToNotifications;
  }

  public Map<String, Boolean> getModuleToggles() {
    return moduleToggles.getToggles();
  }
}
