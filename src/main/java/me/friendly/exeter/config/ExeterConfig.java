package me.friendly.exeter.config;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import me.friendly.api.interfaces.Toggleable;
import me.friendly.exeter.core.Exeter;
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
  private final File debugLog;

  public ExeterConfig() {
    instance = this;
    this.configDir = FabricLoader.getInstance().getConfigDir().resolve("exeter");
    this.tomlReader = new Toml();
    this.tomlWriter = new TomlWriter();
    this.debugLog = FabricLoader.getInstance().getConfigDir().resolve("exeter").resolve("debug.log").toFile();

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

  private void debugLog(String msg) {
    try (PrintWriter pw = new PrintWriter(new java.io.FileWriter(debugLog, true))) {
      pw.println(msg);
    } catch (Exception e) {
      // last resort
    }
  }

  /** Loads all module configurations from TOML files. */
  public void loadAll() {
    debugLog("=== LOAD ALL START ===");
    int count = 0;
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      debugLog("Loading module " + count + ": " + module.getLabel());
      loadModule(module);
      count++;
    }
    debugLog("=== LOAD ALL END (" + count + " modules) ===");
  }

  /** Saves all module configurations to TOML files. */
  public void saveAll() {
    debugLog("=== SAVE ALL START ===");
    int count = 0;
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      debugLog("Saving module " + count + ": " + module.getLabel());
      saveModule(module);
      count++;
    }
    debugLog("=== SAVE ALL END (" + count + " modules) ===");
  }

  /** Loads a single module's configuration from its TOML file. */
  @SuppressWarnings("unchecked")
  public void loadModule(Module module) {
    String fileName = module.getLabel().toLowerCase().replaceAll(" ", "") + ".toml";
    File file = configDir.resolve(fileName).toFile();
    debugLog("loadModule: " + module.getLabel() + " from " + file.getAbsolutePath());

    if (!file.exists()) {
      debugLog("  file does not exist, skipping");
      return;
    }

    try {
      Map<String, Object> data = tomlReader.read(file).toMap();
      debugLog("  raw TOML data keys = " + data.keySet());

      // Load module state (enabled, drawn, keybind)
      if (module instanceof Toggleable && data.containsKey("module")) {
        Map<String, Object> moduleData = (Map<String, Object>) data.get("module");
        ToggleableModule toggleable = (ToggleableModule) module;
        debugLog("  module data = " + moduleData);

        if (moduleData.containsKey("enabled")) {
          Object enabledVal = moduleData.get("enabled");
          debugLog("  enabled raw = " + enabledVal + " (type=" + (enabledVal != null ? enabledVal.getClass().getName() : "null") + ")");
          boolean enabled;
          if (enabledVal instanceof Boolean) {
            enabled = (boolean) enabledVal;
          } else if (enabledVal instanceof Number) {
            enabled = ((Number) enabledVal).intValue() != 0;
          } else {
            enabled = Boolean.parseBoolean(String.valueOf(enabledVal));
          }
          debugLog("  setting enabled = " + enabled + " (was " + toggleable.isRunning() + ")");
          toggleable.setRunning(enabled);
          debugLog("  after setRunning: isRunning = " + toggleable.isRunning());
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
          var kb = Exeter.getInstance().getKeybindManager().getKeybindByLabel(toggleable.getLabel());
          if (kb != null) {
            kb.setKey(keybind);
          }
        }
      }

      // Load properties
      if (data.containsKey("settings")) {
        Map<String, Object> settings = (Map<String, Object>) data.get("settings");
        debugLog("  settings key set = " + settings.keySet());

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
          debugLog("  checking property '" + key + "' containsKey=" + hasKey);
          if (hasKey) {
            Object value = cleaned.get(key);
            debugLog("    loading property " + key + " = " + value + " (type=" + (value != null ? value.getClass().getName() : "null") + ") current=" + property.getValue());
            applyPropertyValue(property, value);
            debugLog("    after load: " + key + " = " + property.getValue());
          } else {
            debugLog("    NOT in settings file, keeping default: " + property.getValue());
          }
        }
      } else {
        debugLog("  no [settings] section in file");
      }

      // Load HUD component positions
      if (module instanceof Hud && data.containsKey("components")) {
        Map<String, Object> components = (Map<String, Object>) data.get("components");
        Hud hud = (Hud) module;
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

  /** Saves a single module's configuration to its TOML file. */
  public void saveModule(Module module) {
    String fileName = module.getLabel().toLowerCase().replaceAll(" ", "") + ".toml";
    File file = configDir.resolve(fileName).toFile();
    debugLog("saveModule: " + module.getLabel() + " -> " + file.getAbsolutePath());

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
        debugLog("  module.enabled = " + toggleable.isRunning());
        debugLog("  module.drawn = " + toggleable.isDrawn());
        debugLog("  module.keybind = " + (keybind != null ? keybind.getKey() : "NULL"));
      }

      // Save properties
      if (!module.getProperties().isEmpty()) {
        Map<String, Object> settings = new HashMap<>();
        for (Property<?> property : module.getProperties()) {
          if (property.getAliases()[0].equals("Drawn")) {
            debugLog("  Skipping Drawn property");
            continue;
          }
          String key = property.getAliases()[0];
          Object value = property.getValue();
          debugLog("  property: " + key + " = " + value + " (type=" + (value != null ? value.getClass().getName() : "null") + ")");
          if (value instanceof Enum) {
            settings.put(key, ((Enum<?>) value).name());
          } else if (value instanceof Float) {
            settings.put(key, ((Float) value).doubleValue());
          } else if (value instanceof Integer) {
            settings.put(key, ((Integer) value).longValue());
          } else {
            settings.put(key, value);
          }
        }
        if (!settings.isEmpty()) {
          data.put("settings", settings);
          debugLog("  settings map size = " + settings.size());
        } else {
          debugLog("  settings map EMPTY");
        }
      } else {
        debugLog("  no properties");
      }

      debugLog("  data map keys = " + data.keySet());
      debugLog("  writing TOML to " + file.getAbsolutePath());
      tomlWriter.write(data, file);
      debugLog("  write SUCCESS for " + module.getLabel());

      // Verify by reading back
      try {
        Map<String, Object> verify = tomlReader.read(file).toMap();
        debugLog("  verify read back keys = " + verify.keySet());
        if (verify.containsKey("settings")) {
          Map<String, Object> verifySettings = (Map<String, Object>) verify.get("settings");
          debugLog("  verify settings = " + verifySettings);
        }
        if (verify.containsKey("module")) {
          Map<String, Object> verifyModule = (Map<String, Object>) verify.get("module");
          debugLog("  verify module = " + verifyModule);
        }
      } catch (Exception ve) {
        debugLog("  verify FAILED: " + ve.getMessage());
      }
    } catch (Exception e) {
      debugLog("FAILED to save config for " + module.getLabel() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }
}
