package me.friendly.exeter.module.impl.toggle.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SearchSelectPopup;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SelectionPopup;
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
  private final SelectionPopup.Toggles moduleToggles = new SelectionPopup.Toggles("Module Toggles");
  private final PopupProperty modulesPopup;

  private List<Module> allModules;

  public Debug() {
    super("Debug", new String[] {"debug"}, 0x00FF00, ModuleType.CLIENT);
    setDescription("Per-module debug logging to file and chat.");
    this.modulesPopup = new PopupProperty("Modules", new Runnable() {
      @Override
      public void run() {
        openModulesPopup();
      }
    });
    offerProperties(
        logToFile,
        logToChat,
        logToNotifications,
        showInfo,
        showWarn,
        showError,
        moduleToggles.getProperty(),
        modulesPopup);
  }

  public void initModuleToggles(Iterable<Module> modules) {
    this.allModules = new ArrayList<Module>();
    for (Module module : modules) {
      if (module == this) continue;
      if (module instanceof ToggleableModule) {
        this.allModules.add(module);
        if (!moduleToggles.getToggles().containsKey(module.getLabel())) {
          moduleToggles.getToggles().put(module.getLabel(), Boolean.FALSE);
        }
      }
    }
  }

  private void openModulesPopup() {
    List<SearchSelectPopup.ToggleItem> items = new ArrayList<SearchSelectPopup.ToggleItem>();
    for (Module module : allModules) {
      final String name = module.getLabel();
      items.add(
          SelectionPopup.toggle(
              name,
              new java.util.function.BooleanSupplier() {
                @Override
                public boolean getAsBoolean() {
                  Boolean value = moduleToggles.getToggles().get(name);
                  return value != null && value.booleanValue();
                }
              },
              new java.util.function.Consumer<Boolean>() {
                @Override
                public void accept(Boolean enabled) {
                  moduleToggles.getToggles().put(name, enabled);
                }
              }));
    }

    SelectionPopup.open(
        "Debug Module Toggles",
        items,
        new Runnable() {
          @Override
          public void run() {
            moduleToggles.save();
            syncSettings();
          }
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
    logger.setLogToFile(logToFile.getValue().booleanValue());
    logger.setLogToChat(logToChat.getValue().booleanValue());
    logger.setLogToNotifications(logToNotifications.getValue().booleanValue());
    logger.setShowInfo(showInfo.getValue().booleanValue());
    logger.setShowWarn(showWarn.getValue().booleanValue());
    logger.setShowError(showError.getValue().booleanValue());

    for (Map.Entry<String, Boolean> entry : moduleToggles.getToggles().entrySet()) {
      logger.setModuleEnabled(entry.getKey(), entry.getValue().booleanValue());
    }
  }
}
