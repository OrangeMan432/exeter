package me.friendly.exeter.module.impl.toggle.client;

import java.util.ArrayList;
import java.util.List;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;

public class Debug extends ToggleableModule {

  private final Property<Boolean> logToFile = new Property<Boolean>(false, "Log To File");
  private final Property<Boolean> logToChat = new Property<Boolean>(false, "Log To Chat");
  private final Property<Boolean> logToNotifications =
      new Property<Boolean>(false, "Log To Notifications");
  private final Property<Boolean> showInfo = new Property<Boolean>(true, "Show Info");
  private final Property<Boolean> showWarn = new Property<Boolean>(true, "Show Warnings");
  private final Property<Boolean> showError = new Property<Boolean>(true, "Show Errors");

  private List<Module> allModules;

  public Debug() {
    super("Debug", new String[] {"debug"}, 0x00FF00, ModuleType.CLIENT);
    setDescription("Per-module debug logging to file and chat.");
    offerProperties(
        logToFile, logToChat, logToNotifications, showInfo, showWarn, showError);
  }

  public void initModuleToggles(Iterable<Module> modules) {
    this.allModules = new ArrayList<Module>();
    for (Module module : modules) {
      if (module == this) continue;
      if (module instanceof ToggleableModule) {
        this.allModules.add(module);
        boolean exists = false;
        for (Property<?> property : getProperties()) {
          if (property.getAliases()[0].equals(module.getLabel())) {
            exists = true;
            break;
          }
        }
        if (!exists) {
          offerProperties(new Property<Boolean>(Boolean.FALSE, module.getLabel()));
        }
      }
    }
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    DebugLogger.get().setEnabled(true);
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

    for (Property<?> property : getProperties()) {
      String alias = property.getAliases()[0];
      if (isReserved(alias)) continue;
      if (property.getValue() instanceof Boolean) {
        logger.setModuleEnabled(
            alias, ((Boolean) property.getValue()).booleanValue());
      }
    }
  }

  private boolean isReserved(String alias) {
    return alias.equals("Log To File")
        || alias.equals("Log To Chat")
        || alias.equals("Log To Notifications")
        || alias.equals("Show Info")
        || alias.equals("Show Warnings")
        || alias.equals("Show Errors")
        || alias.equals("Drawn");
  }
}
