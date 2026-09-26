package me.friendly.exeter.module.impl.toggle.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SearchSelectPopup;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;

public class Debug extends ToggleableModule {

  private final Property<Boolean> logToFile = new Property<Boolean>(false, "Log To File");
  private final Property<Boolean> logToChat = new Property<Boolean>(false, "Log To Chat");
  private final Property<Boolean> logToNotifications =
      new Property<Boolean>(false, "Log To Notifications");
  private final Property<Boolean> showInfo = new Property<Boolean>(true, "Show Info");
  private final Property<Boolean> showWarn = new Property<Boolean>(true, "Show Warnings");
  private final Property<Boolean> showError = new Property<Boolean>(true, "Show Errors");
  private final Property<String> moduleTogglesProp = new Property<>("", "Module Toggles");
  private final PopupProperty modulesPopup;

  private final Map<String, Boolean> moduleToggles = new LinkedHashMap<>();
  private List<Module> allModules;

  public Debug() {
    super("Debug", new String[] {"debug"}, 0x00FF00, ModuleType.CLIENT);
    setDescription("Per-module debug logging to file and chat.");
    this.modulesPopup = new PopupProperty("Modules", this::openModulesPopup);
    offerProperties(
        logToFile, logToChat, logToNotifications, showInfo, showWarn, showError, moduleTogglesProp, modulesPopup);
  }

  public void initModuleToggles(Iterable<Module> modules) {
    this.allModules = new ArrayList<>();
    for (Module module : modules) {
      if (module == this) continue;
      if (module instanceof ToggleableModule) {
        this.allModules.add(module);
        moduleToggles.putIfAbsent(module.getLabel(), false);
      }
    }
  }

  private void openModulesPopup() {
    List<SearchSelectPopup.ToggleItem> items = new ArrayList<>();
    for (Module module : allModules) {
      String name = module.getLabel();
      items.add(
          new SearchSelectPopup.ToggleItem() {
            @Override
            public String getLabel() {
              return name;
            }

            @Override
            public boolean isEnabled() {
              return moduleToggles.getOrDefault(name, false);
            }

            @Override
            public void setEnabled(boolean enabled) {
              moduleToggles.put(name, enabled);
            }
          });
    }

    ClickGui.getClickGui()
        .openPopup(
            new SearchSelectPopup(
                "Debug Module Toggles",
                items,
                () -> {
                  saveToggles();
                  syncSettings();
                  ClickGui.getClickGui().closePopup();
                },
                () -> ClickGui.getClickGui().closePopup()));
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    DebugLogger.get().setEnabled(true);
    loadToggles();
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

    for (Map.Entry<String, Boolean> entry : moduleToggles.entrySet()) {
      logger.setModuleEnabled(entry.getKey(), entry.getValue());
    }
  }

  private void loadToggles() {
    moduleToggles.clear();
    String raw = moduleTogglesProp.getValue();
    if (raw != null && !raw.isEmpty()) {
      for (String pair : raw.split(",")) {
        String trimmed = pair.trim();
        if (trimmed.isEmpty()) continue;
        int eq = trimmed.indexOf(':');
        if (eq > 0) {
          String name = trimmed.substring(0, eq).trim();
          boolean enabled = Boolean.parseBoolean(trimmed.substring(eq + 1).trim());
          moduleToggles.put(name, enabled);
        }
      }
    }
  }

  private void saveToggles() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Boolean> entry : moduleToggles.entrySet()) {
      if (sb.length() > 0) sb.append(",");
      sb.append(entry.getKey()).append(":").append(entry.getValue());
    }
    moduleTogglesProp.setValue(sb.toString());
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
    return moduleToggles;
  }
}
