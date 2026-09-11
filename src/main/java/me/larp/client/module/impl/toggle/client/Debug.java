package me.larp.client.module.impl.toggle.client;

import java.util.LinkedHashMap;
import java.util.Map;
import me.larp.client.logging.DebugLogger;
import me.larp.client.module.Module;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.Property;

public class Debug extends ToggleableModule {

  private final Property<Boolean> logToFile = new Property<Boolean>(false, "Log To File");
  private final Property<Boolean> logToChat = new Property<Boolean>(false, "Log To Chat");

  private final Map<String, Property<Boolean>> moduleToggles = new LinkedHashMap<>();

  public Debug() {
    super("Debug", new String[] {"debug"}, 0x00FF00, ModuleType.CLIENT);
    setDescription("Per-module debug logging to file and chat.");
    offerProperties(logToFile, logToChat);
  }

  /** Called after all modules are registered to create per-module toggle properties. */
  public void initModuleToggles(Iterable<Module> modules) {
    for (Module module : modules) {
      if (module == this) continue;
      if (module instanceof ToggleableModule) {
        Property<Boolean> toggle = new Property<Boolean>(false, module.getLabel());
        moduleToggles.put(module.getLabel(), toggle);
        this.properties.add(toggle);
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
    logger.setLogToFile(logToFile.getValue());
    logger.setLogToChat(logToChat.getValue());

    for (Map.Entry<String, Property<Boolean>> entry : moduleToggles.entrySet()) {
      logger.setModuleEnabled(entry.getKey(), entry.getValue().getValue());
    }
  }

  public Property<Boolean> getLogToFile() {
    return logToFile;
  }

  public Property<Boolean> getLogToChat() {
    return logToChat;
  }

  public Map<String, Property<Boolean>> getModuleToggles() {
    return moduleToggles;
  }
}
