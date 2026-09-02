package me.friendly.exeter.config;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import me.friendly.api.interfaces.Toggleable;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.active.render.Hud;
import me.friendly.exeter.module.impl.toggle.render.hud.HudComponent;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Manages module configuration in TOML format. Each module gets its own .toml file in
 * .minecraft/config/exeter/.
 *
 * <p>Example format:
 *
 * <pre>
 * [module]
 * enabled = false
 * drawn = true
 * keybind = 0
 *
 * [settings]
 * CustomFont = false
 * Watermark = false
 * Casing = "DEFAULT"
 * </pre>
 */
public class ExeterConfig {
  private static ExeterConfig instance;
  private final Path configDir;
  private final Toml tomlReader;
  private final TomlWriter tomlWriter;

  public ExeterConfig() {
    instance = this;
    this.configDir = FabricLoader.getInstance().getConfigDir().resolve("exeter");
    this.tomlReader = new Toml();
    this.tomlWriter = new TomlWriter();

    try {
      Files.createDirectories(configDir);
    } catch (IOException e) {
      System.err.println("[Exeter] Failed to create config directory: " + e.getMessage());
    }
  }

  public static ExeterConfig getInstance() {
    return instance;
  }

  public Path getConfigDir() {
    return configDir;
  }

  /** Loads all module configurations from TOML files. */
  public void loadAll() {
    DebugLogger.get().logSystem("Config", "=== LOAD ALL START ===");
    int count = 0;
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
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
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      DebugLogger.get().logSystem("Config", "Saving module " + count + ": " + module.getLabel());
      saveModule(module);
      count++;
    }
    DebugLogger.get().logSystem("Config", "=== SAVE ALL END (" + count + " modules) ===");
  }

  /** Loads a single module's configuration from its TOML file. */
  @SuppressWarnings("unchecked")
  public void loadModule(Module module) {
    String fileName = module.getLabel().toLowerCase().replaceAll(" ", "") + ".toml";
    File file = configDir.resolve(fileName).toFile();
    DebugLogger.get()
        .logSystem(
            "Config", "loadModule: " + module.getLabel() + " from " + file.getAbsolutePath());

    if (!file.exists()) {
      DebugLogger.get().logSystem("Config", "  file does not exist, skipping");
      return;
    }

    try {
      Map<String, Object> data = tomlReader.read(file).toMap();
      DebugLogger.get().logSystem("Config", "  raw TOML data keys = " + data.keySet());

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

        if (moduleData.containsKey("drawn")) {
          Object drawnVal = moduleData.get("drawn");
          boolean drawn;
          if (drawnVal instanceof Boolean) {
            drawn = (boolean) drawnVal;
          } else if (drawnVal instanceof Number) {
            drawn = ((Number) drawnVal).intValue() != 0;
          } else {
            drawn = Boolean.parseBoolean(String.valueOf(drawnVal));
          }
          toggleable.setDrawn(drawn);
        }

        if (moduleData.containsKey("keybind")) {
          int keybind = ((Number) moduleData.get("keybind")).intValue();
          var kb =
              Exeter.getInstance().getKeybindManager().getKeybindByLabel(toggleable.getLabel());
          if (kb != null) {
            kb.setKey(keybind);
          }
        }
      }

      // Load properties
      if (data.containsKey("settings")) {
        Map<String, Object> settings = (Map<String, Object>) data.get("settings");
        DebugLogger.get().logSystem("Config", "  settings key set = " + settings.keySet());

        // Strip TOML quotes from map keys (toml4j includes them)
        Map<String, Object> cleaned = new HashMap<>();
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
          String k = entry.getKey();
          if (k.startsWith("\"") && k.endsWith("\"")) {
            k = k.substring(1, k.length() - 1);
          }
          cleaned.put(k, entry.getValue());
        }

        for (Property<?> property : module.getProperties()) {
          String key = property.getAliases()[0];
          boolean hasKey = cleaned.containsKey(key);
          DebugLogger.get()
              .logSystem("Config", "  checking property '" + key + "' containsKey=" + hasKey);
          if (hasKey) {
            Object value = cleaned.get(key);
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
            if (cleaned.containsKey(childFlatKey)) {
              Object childValue = cleaned.get(childFlatKey);
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
        Map<String, Object> rawComponents = (Map<String, Object>) data.get("components");
        Hud hud = (Hud) module;

        // Strip TOML quotes from keys
        Map<String, Object> components = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawComponents.entrySet()) {
          String k = entry.getKey();
          if (k.startsWith("\"") && k.endsWith("\"")) {
            k = k.substring(1, k.length() - 1);
          }
          components.put(k, entry.getValue());
        }

        for (HudComponent comp : hud.getHudComponents()) {
          if (components.containsKey(comp.getLabel())) {
            Map<String, Object> pos = (Map<String, Object>) components.get(comp.getLabel());
            if (pos.containsKey("x")) {
              comp.setX(((Long) pos.get("x")).intValue());
            }
            if (pos.containsKey("y")) {
              comp.setY(((Long) pos.get("y")).intValue());
            }
          }
        }
      }
    } catch (Exception e) {
      System.err.println(
          "[Exeter] Failed to load config for " + module.getLabel() + ": " + e.getMessage());
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
    if (key.equals("Drawn")) {
      return;
    }
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
    } else if (value instanceof Float) {
      settings.put(key, ((Float) value).doubleValue());
    } else if (value instanceof Integer) {
      settings.put(key, ((Integer) value).longValue());
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
      } else if (childValue instanceof Float) {
        settings.put(childKey, ((Float) childValue).doubleValue());
      } else if (childValue instanceof Integer) {
        settings.put(childKey, ((Integer) childValue).longValue());
      } else {
        settings.put(childKey, childValue);
      }
    }
  }

  /** Saves a single module's configuration to its TOML file. */
  public void saveModule(Module module) {
    String fileName = module.getLabel().toLowerCase().replaceAll(" ", "") + ".toml";
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
        moduleData.put("drawn", toggleable.isDrawn());
        var keybind =
            Exeter.getInstance().getKeybindManager().getKeybindByLabel(toggleable.getLabel());
        moduleData.put("keybind", (long) (keybind != null ? keybind.getKey() : 0));
        data.put("module", moduleData);
        DebugLogger.get().logSystem("Config", "  module.enabled = " + toggleable.isRunning());
        DebugLogger.get().logSystem("Config", "  module.drawn = " + toggleable.isDrawn());
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
          pos.put("x", (long) comp.getX());
          pos.put("y", (long) comp.getY());
          components.put(comp.getLabel(), pos);
        }
        if (!components.isEmpty()) {
          data.put("components", components);
          DebugLogger.get().logSystem("Config", "  HUD components saved: " + components.keySet());
        }
      }

      DebugLogger.get().logSystem("Config", "  writing TOML to " + file.getAbsolutePath());
      tomlWriter.write(data, file);
      DebugLogger.get().logSystem("Config", "  write SUCCESS for " + module.getLabel());
    } catch (Exception e) {
      System.err.println(
          "[Exeter] Failed to save config for " + module.getLabel() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }
}
