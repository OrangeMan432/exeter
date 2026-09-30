package me.friendly.exeter.config;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import me.friendly.api.interfaces.Toggleable;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.keybind.Keybind;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.properties.ActionProperty;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;

/**
 * Manages module configuration in TOML format (newbase parity). Each module gets its own .toml
 * file in the modules directory.
 *
 * <pre>
 * [module]
 * enabled = false
 * keybind = 0
 *
 * [settings]
 * Factor = 1.5
 * Mode = "FAST"
 *
 * [hud]
 * x = 5
 * y = 5
 * corner = "TOP_LEFT"
 * </pre>
 */
public class ExeterConfig {
  private static ExeterConfig instance;
  private final File configDir;
  private final Toml tomlReader;
  private final TomlWriter tomlWriter;

  public ExeterConfig() {
    instance = this;
    this.configDir = new File(Exeter.getInstance().getDirectory(), "modules");
    this.tomlReader = new Toml();
    this.tomlWriter = new TomlWriter();

    try {
      Files.createDirectories(configDir.toPath());
    } catch (IOException e) {
      System.err.println("[Exeter] Failed to create config directory: " + e.getMessage());
    }
  }

  public static ExeterConfig getInstance() {
    return instance;
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
    DebugLogger.get().logFile("Config", "=== SAVE ALL START ===");
    int count = 0;
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      DebugLogger.get().logFile("Config", "Saving module " + count + ": " + module.getLabel());
      saveModule(module);
      count++;
    }
    DebugLogger.get().logFile("Config", "=== SAVE ALL END (" + count + " modules) ===");
  }

  /** Loads a single module's configuration from its TOML file. */
  @SuppressWarnings({"unchecked"})
  public void loadModule(Module module) {
    String fileName = module.getLabel().toLowerCase().replaceAll(" ", "") + ".toml";
    File file = new File(configDir, fileName);
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

      // Load module state (enabled, keybind). The enabled flag is applied after
      // settings are loaded so onEnable() observes the restored property values.
      boolean restoreEnabled = false;
      boolean hasEnabledState = false;
      if (module instanceof Toggleable && data.containsKey("module")) {
        Map<String, Object> moduleData = (Map<String, Object>) data.get("module");
        ToggleableModule toggleable = (ToggleableModule) module;
        DebugLogger.get().logSystem("Config", "  module data = " + moduleData);

        if (moduleData.containsKey("enabled")) {
          Object enabledVal = moduleData.get("enabled");
          boolean enabled;
          if (enabledVal instanceof Boolean) {
            enabled = ((Boolean) enabledVal).booleanValue();
          } else if (enabledVal instanceof Number) {
            enabled = ((Number) enabledVal).intValue() != 0;
          } else {
            enabled = Boolean.parseBoolean(String.valueOf(enabledVal));
          }
          restoreEnabled = enabled;
          hasEnabledState = true;
        }

        if (moduleData.containsKey("keybind")) {
          int keybind = ((Number) moduleData.get("keybind")).intValue();
          Keybind kb =
              Exeter.getInstance().getKeybindManager().getKeybindByLabel(toggleable.getLabel());
          if (kb != null && keybind != 0) {
            kb.setKey(keybind);
          }
        }
      }

      // Load properties
      if (data.containsKey("settings")) {
        Map<String, Object> settings = (Map<String, Object>) data.get("settings");

        // Strip TOML quotes from map keys (toml4j includes them)
        Map<String, Object> cleaned = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
          String k = entry.getKey();
          if (k.startsWith("\"") && k.endsWith("\"")) {
            k = k.substring(1, k.length() - 1);
          }
          cleaned.put(k, entry.getValue());
        }

        for (Property<?> property : module.getProperties()) {
          if (property instanceof ActionProperty || property instanceof PopupProperty) {
            continue;
          }
          String key = property.getAliases()[0];
          if (!cleaned.containsKey(key)) {
            continue;
          }
          Object value = cleaned.get(key);
          if (value instanceof Map) {
            Map<String, Object> childMap = (Map<String, Object>) value;
            for (Property<?> child : property.getChildren()) {
              String childKey = child.getAliases()[0];
              if (childMap.containsKey(childKey)) {
                applyPropertyValue(child, childMap.get(childKey));
              }
            }
          } else {
            applyPropertyValue(property, value);
          }
        }

        // Second pass: handle __ separated child keys
        for (Property<?> property : module.getProperties()) {
          String parentKey = property.getAliases()[0];
          for (Property<?> child : property.getChildren()) {
            String childFlatKey = parentKey + "__" + child.getAliases()[0];
            if (cleaned.containsKey(childFlatKey)) {
              applyPropertyValue(child, cleaned.get(childFlatKey));
            }
          }
        }
      }

      // Apply enabled state after settings so onEnable() observes restored values.
      if (hasEnabledState && module instanceof ToggleableModule) {
        ((ToggleableModule) module).setRunning(restoreEnabled);
      }

      // Load HUD module position and corner
      if (module instanceof HudModule && data.containsKey("hud")) {
        HudModule hudModule = (HudModule) module;
        Map<String, Object> hudData = (Map<String, Object>) data.get("hud");

        if (hudData.containsKey("x")) {
          hudModule.setX(((Number) hudData.get("x")).intValue());
        }
        if (hudData.containsKey("y")) {
          hudModule.setY(((Number) hudData.get("y")).intValue());
        }
        if (hudData.containsKey("corner")) {
          String cornerName = hudData.get("corner").toString();
          try {
            hudModule.setCorner(HudModule.Corner.valueOf(cornerName));
          } catch (IllegalArgumentException e) {
            DebugLogger.get().logSystem("Config", "  invalid corner value: " + cornerName);
          }
        }
      }

      // Load saved window positions (applied when the Windows screen opens)
      if (module instanceof me.friendly.exeter.module.impl.toggle.client.WindowsModule
          && data.containsKey("windows")) {
        Map<String, Object> windowsData = (Map<String, Object>) data.get("windows");
        Map<String, int[]> positions = new HashMap<String, int[]>();
        for (Map.Entry<String, Object> entry : windowsData.entrySet()) {
          if (entry.getValue() instanceof Map) {
            int[] pos = readPosition((Map<String, Object>) entry.getValue());
            if (pos != null) {
              positions.put(entry.getKey(), pos);
            }
          }
        }
        ((me.friendly.exeter.module.impl.toggle.client.WindowsModule) module)
            .setPendingPositions(positions);
      }

      // Load ClickGUI panel positions
      if (module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui
          && data.containsKey("panels")) {
        Map<String, Object> panelsData = (Map<String, Object>) data.get("panels");
        Map<String, int[]> positions = new HashMap<String, int[]>();
        for (Map.Entry<String, Object> entry : panelsData.entrySet()) {
          if (entry.getValue() instanceof Map) {
            int[] pos = readPosition((Map<String, Object>) entry.getValue());
            if (pos != null) {
              positions.put(entry.getKey(), pos);
            }
          }
        }
        ((me.friendly.exeter.module.impl.toggle.render.ClickGui) module)
            .setPendingPanels(positions);
      }
    } catch (Exception e) {
      System.err.println(
          "[Exeter] Failed to load config for " + module.getLabel() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Reads an {x, y} position table, or null when malformed. */
  private static int[] readPosition(Map<String, Object> table) {
    try {
      Object x = table.get("x");
      Object y = table.get("y");
      if (x instanceof Number && y instanceof Number) {
        return new int[] {((Number) x).intValue(), ((Number) y).intValue()};
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  /** Saves a single module's configuration to its TOML file. */
  public void saveModule(Module module) {
    String fileName = module.getLabel().toLowerCase().replaceAll(" ", "") + ".toml";
    File file = new File(configDir, fileName);
    DebugLogger.get()
        .logFile("Config", "saveModule: " + module.getLabel() + " -> " + file.getAbsolutePath());

    try {
      Map<String, Object> data = new HashMap<String, Object>();

      // Save module state
      if (module instanceof Toggleable) {
        ToggleableModule toggleable = (ToggleableModule) module;
        Map<String, Object> moduleData = new HashMap<String, Object>();
        moduleData.put("enabled", Boolean.valueOf(toggleable.isRunning()));
        Keybind keybind =
            Exeter.getInstance().getKeybindManager().getKeybindByLabel(toggleable.getLabel());
        moduleData.put("keybind", Long.valueOf(keybind != null ? keybind.getKey() : 0));
        data.put("module", moduleData);
      }

      // Save properties (including children)
      if (!module.getProperties().isEmpty()) {
        Map<String, Object> settings = new HashMap<String, Object>();
        for (Property<?> property : module.getProperties()) {
          if (property instanceof ActionProperty || property instanceof PopupProperty) {
            continue;
          }
          savePropertyRecursive(property, settings);
        }
        if (!settings.isEmpty()) {
          data.put("settings", settings);
        }
      }

      // Save HUD module position and corner
      if (module instanceof HudModule) {
        HudModule hudModule = (HudModule) module;
        Map<String, Object> hudData = new HashMap<String, Object>();
        hudData.put("x", Long.valueOf(hudModule.getX()));
        hudData.put("y", Long.valueOf(hudModule.getY()));
        hudData.put("corner", hudModule.getCorner().name());
        data.put("hud", hudData);
      }

      // Save window positions
      if (module instanceof me.friendly.exeter.module.impl.toggle.client.WindowsModule) {
        me.friendly.exeter.module.impl.toggle.client.WindowsModule windowsModule =
            (me.friendly.exeter.module.impl.toggle.client.WindowsModule) module;
        Map<String, Object> windowsData = new HashMap<String, Object>();
        for (Map.Entry<String, int[]> entry : windowsModule.getPendingPositions().entrySet()) {
          Map<String, Object> pos = new HashMap<String, Object>();
          pos.put("x", Long.valueOf(entry.getValue()[0]));
          pos.put("y", Long.valueOf(entry.getValue()[1]));
          windowsData.put(entry.getKey(), pos);
        }
        data.put("windows", windowsData);
      }

      // Save ClickGUI panel positions
      if (module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui) {
        me.friendly.exeter.module.impl.toggle.render.ClickGui clickGuiModule =
            (me.friendly.exeter.module.impl.toggle.render.ClickGui) module;
        Map<String, Object> panelsData = new HashMap<String, Object>();
        for (Map.Entry<String, int[]> entry : clickGuiModule.getPendingPanels().entrySet()) {
          Map<String, Object> pos = new HashMap<String, Object>();
          pos.put("x", Long.valueOf(entry.getValue()[0]));
          pos.put("y", Long.valueOf(entry.getValue()[1]));
          panelsData.put(entry.getKey(), pos);
        }
        data.put("panels", panelsData);
      }

      tomlWriter.write(data, file);
    } catch (Exception e) {
      System.err.println(
          "[Exeter] Failed to save config for " + module.getLabel() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void applyPropertyValue(Property<?> property, Object value) {
    if (property instanceof EnumProperty) {
      ((EnumProperty) property).setValue(value.toString());
    } else if (property instanceof NumberProperty) {
      if (property.getValue() instanceof Integer) {
        ((NumberProperty) property).setValue(Integer.valueOf(((Number) value).intValue()));
      } else if (property.getValue() instanceof Float) {
        ((NumberProperty) property).setValue(Float.valueOf(((Number) value).floatValue()));
      } else if (property.getValue() instanceof Double) {
        ((NumberProperty) property).setValue(Double.valueOf(((Number) value).doubleValue()));
      } else if (property.getValue() instanceof Long) {
        ((NumberProperty) property).setValue(Long.valueOf(((Number) value).longValue()));
      }
    } else if (property.getValue() instanceof Boolean) {
      if (value instanceof Boolean) {
        ((Property<Boolean>) property).setValue((Boolean) value);
      } else if (value instanceof String) {
        ((Property<Boolean>) property).setValue(Boolean.parseBoolean((String) value));
      } else if (value instanceof Number) {
        ((Property<Boolean>) property).setValue(
            Boolean.valueOf(((Number) value).intValue() != 0));
      }
    } else if (property.getValue() instanceof String) {
      ((Property<Object>) property).setValue(value);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void savePropertyRecursive(Property<?> property, Map<String, Object> settings) {
    if (property instanceof ActionProperty || property instanceof PopupProperty) {
      return;
    }
    String key = property.getAliases()[0];
    Object value = property.getValue();
    if (value instanceof Enum) {
      settings.put(key, ((Enum<?>) value).name());
    } else if (value instanceof Float) {
      settings.put(key, Double.valueOf(((Float) value).doubleValue()));
    } else if (value instanceof Integer) {
      settings.put(key, Long.valueOf(((Integer) value).longValue()));
    } else {
      settings.put(key, value);
    }

    for (Property<?> child : property.getChildren()) {
      String childKey = key + "__" + child.getAliases()[0];
      Object childValue = child.getValue();
      if (childValue instanceof Enum) {
        settings.put(childKey, ((Enum<?>) childValue).name());
      } else if (childValue instanceof Float) {
        settings.put(childKey, Double.valueOf(((Float) childValue).doubleValue()));
      } else if (childValue instanceof Integer) {
        settings.put(childKey, Long.valueOf(((Integer) childValue).longValue()));
      } else {
        settings.put(childKey, childValue);
      }
    }
  }

  private File fileFor(Module module) {
    return new File(
        configDir, module.getLabel().toLowerCase().replaceAll(" ", "") + ".toml");
  }
}
