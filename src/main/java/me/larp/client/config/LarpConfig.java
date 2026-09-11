package me.larp.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import me.larp.api.interfaces.Toggleable;
import me.larp.client.core.Larp;
import me.larp.client.logging.DebugLogger;
import me.larp.client.module.Module;
import me.larp.client.module.ToggleableModule;
import me.larp.client.module.impl.active.render.Hud;
import me.larp.client.module.impl.toggle.render.hud.HudComponent;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Manages module configuration in JSON format. Each module gets its own .json file in
 * .minecraft/config/larp/.
 *
 * <p>Example format:
 *
 * <pre>
 * {
 *   "module": { "enabled": false, "keybind": 0 },
 *   "settings": { "Range": 4.5, "Rotate": true, "Mode": "BOOST" }
 * }
 * </pre>
 */
public class LarpConfig {
  private static LarpConfig instance;
  private final Path configDir;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public LarpConfig() {
    instance = this;
    this.configDir = FabricLoader.getInstance().getConfigDir().resolve("larp");

    try {
      Files.createDirectories(configDir);
    } catch (IOException e) {
      System.err.println("[Larp] Failed to create config directory: " + e.getMessage());
    }
  }

  public static LarpConfig getInstance() {
    return instance;
  }

  public Path getConfigDir() {
    return configDir;
  }

  /** Loads all module configurations from TOML files. */
  public void loadAll() {
    DebugLogger.get().logSystem("Config", "=== LOAD ALL START ===");
    int count = 0;
    for (Module module : Larp.getInstance().getModuleManager().getRegistry()) {
      DebugLogger.get().logSystem("Config", "Loading module " + count + ": " + module.getLabel());
      loadModule(module);
      count++;
    }
    DebugLogger.get().logSystem("Config", "=== LOAD ALL END (" + count + " modules) ===");
  }

  /** Saves all module configurations to TOML files. */
  public void saveAll() {
    DebugLogger.get().logSystem("Config", "=== SAVE ALL START ===");
    int count = 0;
    for (Module module : Larp.getInstance().getModuleManager().getRegistry()) {
      DebugLogger.get().logSystem("Config", "Saving module " + count + ": " + module.getLabel());
      saveModule(module);
      count++;
    }
    DebugLogger.get().logSystem("Config", "=== SAVE ALL END (" + count + " modules) ===");
  }

  /** Loads a single module's configuration from its JSON file. */
  @SuppressWarnings("unchecked")
  public void loadModule(Module module) {
    String fileName = module.getLabel().toLowerCase().replaceAll(" ", "") + ".json";
    File file = configDir.resolve(fileName).toFile();
    DebugLogger.get()
        .logSystem(
            "Config", "loadModule: " + module.getLabel() + " from " + file.getAbsolutePath());

    if (!file.exists()) {
      DebugLogger.get().logSystem("Config", "  file does not exist, skipping");
      return;
    }

    try {
      Map<String, Object> data =
          gson.fromJson(Files.readString(file.toPath()), Map.class);
      if (data == null) return;
      DebugLogger.get().logSystem("Config", "  raw JSON data keys = " + data.keySet());

      // Load module state (enabled, drawn, keybind)
      if (module instanceof Toggleable && data.containsKey("module")) {
        Map<String, Object> moduleData = (Map<String, Object>) data.get("module");
        ToggleableModule toggleable = (ToggleableModule) module;
        DebugLogger.get().logSystem("Config", "  module data = " + moduleData);

        if (moduleData.containsKey("enabled")) {
          Object enabledVal = moduleData.get("enabled");
          DebugLogger.get()
              .logSystem(
                  "Config",
                  "  enabled raw = "
                      + enabledVal
                      + " (type="
                      + (enabledVal != null ? enabledVal.getClass().getName() : "null")
                      + ")");
          boolean enabled;
          if (enabledVal instanceof Boolean) {
            enabled = (boolean) enabledVal;
          } else if (enabledVal instanceof Number) {
            enabled = ((Number) enabledVal).intValue() != 0;
          } else {
            enabled = Boolean.parseBoolean(String.valueOf(enabledVal));
          }
          DebugLogger.get()
              .logSystem(
                  "Config",
                  "  setting enabled = " + enabled + " (was " + toggleable.isRunning() + ")");
          toggleable.setRunning(enabled);
          DebugLogger.get()
              .logSystem("Config", "  after setRunning: isRunning = " + toggleable.isRunning());
        }

        if (moduleData.containsKey("keybind")) {
          int keybind = ((Number) moduleData.get("keybind")).intValue();
          var kb =
              Larp.getInstance().getKeybindManager().getKeybindByLabel(toggleable.getLabel());
          if (kb != null) {
            kb.setKey(keybind);
          }
        }
      }

      // Load properties (gson keys need no quote stripping)
      if (data.containsKey("settings")) {
        Map<String, Object> settings = (Map<String, Object>) data.get("settings");
        DebugLogger.get().logSystem("Config", "  settings key set = " + settings.keySet());

        for (Property<?> property : module.getProperties()) {
          String key = property.getAliases()[0];
          boolean hasKey = settings.containsKey(key);
          DebugLogger.get()
              .logSystem("Config", "  checking property '" + key + "' containsKey=" + hasKey);
          if (hasKey) {
            Object value = settings.get(key);
            DebugLogger.get()
                .logSystem(
                    "Config",
                    "    loading "
                        + key
                        + " = "
                        + value
                        + " (type="
                        + (value != null ? value.getClass().getName() : "null")
                        + ") current="
                        + property.getValue());

            if (value instanceof Map) {
              DebugLogger.get()
                  .logSystem("Config", "    value is nested map, loading children");
              Map<String, Object> childMap = (Map<String, Object>) value;
              for (Property<?> child : property.getChildren()) {
                String childKey = child.getAliases()[0];
                if (childMap.containsKey(childKey)) {
                  Object childValue = childMap.get(childKey);
                  DebugLogger.get()
                      .logSystem(
                          "Config",
                          "    loading child "
                              + childKey
                              + " = "
                              + childValue
                              + " (type="
                              + (childValue != null ? childValue.getClass().getName() : "null")
                              + ")");
                  applyPropertyValue(child, childValue);
                  DebugLogger.get()
                      .logSystem(
                          "Config",
                          "    after load child: " + childKey + " = " + child.getValue());
                }
              }
            } else {
              applyPropertyValue(property, value);
              DebugLogger.get()
                  .logSystem("Config", "    after load: " + key + " = " + property.getValue());
            }
          } else {
            DebugLogger.get()
                .logSystem(
                    "Config", "    NOT in settings file, keeping default: " + property.getValue());
          }
        }

        // Second pass: handle __ separated child keys
        for (Property<?> property : module.getProperties()) {
          String parentKey = property.getAliases()[0];
          for (Property<?> child : property.getChildren()) {
            String childFlatKey = parentKey + "__" + child.getAliases()[0];
            if (settings.containsKey(childFlatKey)) {
              Object childValue = settings.get(childFlatKey);
              DebugLogger.get()
                  .logSystem(
                      "Config",
                      "    loading child (flat key) "
                          + childFlatKey
                          + " = "
                          + childValue
                          + " (type="
                          + (childValue != null ? childValue.getClass().getName() : "null")
                          + ")");
              applyPropertyValue(child, childValue);
              DebugLogger.get()
                  .logSystem(
                      "Config",
                      "    after load child: " + child.getAliases()[0] + " = " + child.getValue());
            }
          }
        }
      } else {
        DebugLogger.get().logSystem("Config", "  no [settings] section in file");
      }

      // Load HUD component positions
      if (module instanceof Hud && data.containsKey("components")) {
        Hud hud = (Hud) module;
        Map<String, Object> components = (Map<String, Object>) data.get("components");

        for (HudComponent comp : hud.getHudComponents()) {
          if (components.containsKey(comp.getLabel())) {
            Map<String, Object> pos = (Map<String, Object>) components.get(comp.getLabel());
            if (pos.containsKey("x")) {
              comp.setX(((Number) pos.get("x")).intValue());
            }
            if (pos.containsKey("y")) {
              comp.setY(((Number) pos.get("y")).intValue());
            }
          }
        }
      }
    } catch (Exception e) {
      System.err.println(
          "[Larp] Failed to load config for " + module.getLabel() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void applyPropertyValue(Property<?> property, Object value) {
    if (property instanceof EnumProperty) {
      ((EnumProperty) property).setValue(value.toString());
    } else if (property instanceof NumberProperty) {
      if (property.getValue() instanceof Integer) {
        ((NumberProperty) property).setValue(((Number) value).intValue());
      } else if (property.getValue() instanceof Float) {
        ((NumberProperty) property).setValue(((Number) value).floatValue());
      } else if (property.getValue() instanceof Double) {
        ((NumberProperty) property).setValue(((Number) value).doubleValue());
      } else if (property.getValue() instanceof Long) {
        ((NumberProperty) property).setValue(((Number) value).longValue());
      }
    } else if (property.getValue() instanceof Boolean) {
      if (value instanceof Boolean) {
        ((Property<Boolean>) property).setValue((Boolean) value);
      } else if (value instanceof String) {
        ((Property<Boolean>) property).setValue(Boolean.parseBoolean((String) value));
      } else if (value instanceof Number) {
        ((Property<Boolean>) property).setValue(((Number) value).intValue() != 0);
      }
    } else if (property.getValue() instanceof String) {
      ((Property<Object>) property).setValue(value);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void savePropertyRecursive(Property<?> property, Map<String, Object> settings) {
    String key = property.getAliases()[0];
    Object value = property.getValue();
    DebugLogger.get()
        .logSystem(
            "Config",
            "  property: "
                + key
                + " = "
                + value
                + " (type="
                + (value != null ? value.getClass().getName() : "null")
                + ")");
    if (value instanceof Enum) {
      settings.put(key, ((Enum<?>) value).name());
    } else {
      settings.put(key, value);
    }

    for (Property<?> child : property.getChildren()) {
      String childKey = key + "__" + child.getAliases()[0];
      Object childValue = child.getValue();
      DebugLogger.get()
          .logSystem(
              "Config",
              "  child property: "
                  + childKey
                  + " = "
                  + childValue
                  + " (type="
                  + (childValue != null ? childValue.getClass().getName() : "null")
                  + ")");
      if (childValue instanceof Enum) {
        settings.put(childKey, ((Enum<?>) childValue).name());
      } else {
        settings.put(childKey, childValue);
      }
    }
  }

  /** Saves a single module's configuration to its JSON file. */
  public void saveModule(Module module) {
    String fileName = module.getLabel().toLowerCase().replaceAll(" ", "") + ".json";
    File file = configDir.resolve(fileName).toFile();
    DebugLogger.get()
        .logSystem("Config", "saveModule: " + module.getLabel() + " -> " + file.getAbsolutePath());

    try {
      Map<String, Object> data = new HashMap<>();

      // Save module state
      if (module instanceof Toggleable) {
        ToggleableModule toggleable = (ToggleableModule) module;
        Map<String, Object> moduleData = new HashMap<>();
        moduleData.put("enabled", toggleable.isRunning());
        var keybind =
            Larp.getInstance().getKeybindManager().getKeybindByLabel(toggleable.getLabel());
        moduleData.put("keybind", (long) (keybind != null ? keybind.getKey() : 0));
        data.put("module", moduleData);
        DebugLogger.get().logSystem("Config", "  module.enabled = " + toggleable.isRunning());
        DebugLogger.get()
            .logSystem(
                "Config", "  module.keybind = " + (keybind != null ? keybind.getKey() : "NULL"));
      }

      // Save properties (including children)
      if (!module.getProperties().isEmpty()) {
        Map<String, Object> settings = new HashMap<>();
        for (Property<?> property : module.getProperties()) {
          savePropertyRecursive(property, settings);
        }
        if (!settings.isEmpty()) {
          data.put("settings", settings);
          DebugLogger.get().logSystem("Config", "  settings map size = " + settings.size());
        }
      }

      // Save HUD component positions
      if (module instanceof Hud) {
        Hud hud = (Hud) module;
        Map<String, Object> components = new HashMap<>();
        for (HudComponent comp : hud.getHudComponents()) {
          Map<String, Object> pos = new HashMap<>();
          pos.put("x", comp.getX());
          pos.put("y", comp.getY());
          components.put(comp.getLabel(), pos);
        }
        if (!components.isEmpty()) {
          data.put("components", components);
          DebugLogger.get().logSystem("Config", "  HUD components saved: " + components.keySet());
        }
      }

      DebugLogger.get().logSystem("Config", "  writing JSON to " + file.getAbsolutePath());
      Files.writeString(file.toPath(), gson.toJson(data));
      DebugLogger.get().logSystem("Config", "  write SUCCESS for " + module.getLabel());
    } catch (Exception e) {
      System.err.println(
          "[Larp] Failed to save config for " + module.getLabel() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }
}
