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
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
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
 *
 * [hud]
 * x = 5
 * y = 5
 * corner = "TOP_LEFT"
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
    int count = 0;
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      loadModule(module);
      count++;
    }
  }

  /** Saves all module configurations to TOML files. */
  public void saveAll() {
    int count = 0;
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      saveModule(module);
      count++;
    }
  }

  /** Loads a single module's configuration from its TOML file. */
  @SuppressWarnings("unchecked")
  public void loadModule(Module module) {
    String fileName = module.getLabel().toLowerCase().replaceAll(" ", "") + ".toml";
    File file = configDir.resolve(fileName).toFile();

    if (!file.exists()) {
      return;
    }

    try {
      Map<String, Object> data = tomlReader.read(file).toMap();

      // Load module state (enabled, drawn, keybind). The enabled flag is applied after
      // settings are loaded so onEnable() observes the restored property values.
      boolean restoreEnabled = false;
      boolean hasEnabledState = false;
      if (module instanceof Toggleable && data.containsKey("module")) {
        Map<String, Object> moduleData = (Map<String, Object>) data.get("module");
        ToggleableModule toggleable = (ToggleableModule) module;

        if (moduleData.containsKey("enabled")) {
          Object enabledVal = moduleData.get("enabled");
          boolean enabled;
          if (enabledVal instanceof Boolean) {
            enabled = (boolean) enabledVal;
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
          // Pre-26.3 configs store GLFW keycodes; migrate to SDL codes once.
          Object marker = moduleData.get("keycodes");
          if (marker == null) {
            for (Map.Entry<String, Object> e : moduleData.entrySet()) {
              String k = e.getKey();
              if (k.startsWith("\"") && k.endsWith("\"")) {
                k = k.substring(1, k.length() - 1);
              }
              if (k.equals("keycodes")) {
                marker = e.getValue();
                break;
              }
            }
          }
          if (!"sdl".equals(String.valueOf(marker))) {
            int migrated = me.friendly.exeter.keybind.Keybind.migrateLegacyGlfwKey(keybind);
            DebugLogger.get()
                .logSystem("Config", "  migrating legacy keybind " + keybind + " -> " + migrated);
            keybind = migrated;
          }
          var kb =
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
          if (hasKey) {
            Object value = cleaned.get(key);

            if (value instanceof Map) {
              Map<String, Object> childMap = (Map<String, Object>) value;
              for (Property<?> child : property.getChildren()) {
                String childKey = child.getAliases()[0];
                if (childMap.containsKey(childKey)) {
                  Object childValue = childMap.get(childKey);
                  applyPropertyValue(child, childValue);
                }
              }
            } else {
              applyPropertyValue(property, value);
            }
          } else {
          }
        }

        // Second pass: handle __ separated child keys
        for (Property<?> property : module.getProperties()) {
          String parentKey = property.getAliases()[0];
          for (Property<?> child : property.getChildren()) {
            String childFlatKey = parentKey + "__" + child.getAliases()[0];
            if (cleaned.containsKey(childFlatKey)) {
              Object childValue = cleaned.get(childFlatKey);
              applyPropertyValue(child, childValue);
            }
          }
        }
      } else {
      }

      // Apply enabled state after settings so onEnable() observes restored values.
      if (hasEnabledState && module instanceof ToggleableModule) {
        ToggleableModule toggleable = (ToggleableModule) module;
        toggleable.setRunning(restoreEnabled);
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
        if (hudData.containsKey("free")) {
          hudModule.setFree(Boolean.parseBoolean(hudData.get("free").toString()));
        }
      }

      // Load saved window positions (applied when the Windows screen opens)
      if (module instanceof me.friendly.exeter.module.impl.toggle.client.WindowsModule windowsModule
          && data.containsKey("windows")) {
        Map<String, Object> windowsData = (Map<String, Object>) data.get("windows");
        Map<String, int[]> positions = new HashMap<>();
        Map<String, Boolean> hidden = new HashMap<>();
        for (Map.Entry<String, Object> entry : windowsData.entrySet()) {
          if (entry.getValue() instanceof Map) {
            Map<String, Object> windowEntry = (Map<String, Object>) entry.getValue();
            int[] pos = readPosition(windowEntry);
            if (pos != null) {
              positions.put(entry.getKey(), pos);
            }
            if (windowEntry.containsKey("hidden")) {
              hidden.put(
                  entry.getKey(), Boolean.parseBoolean(windowEntry.get("hidden").toString()));
            }
          }
        }
        windowsModule.setPendingPositions(positions);
        windowsModule.setPendingHidden(hidden);
      }

      // Load saved ClickGUI panel positions (applied when the ClickGUI screen opens)
      if (module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui clickGuiModule
          && data.containsKey("panels")) {
        Map<String, Object> panelsData = (Map<String, Object>) data.get("panels");
        Map<String, int[]> positions = new HashMap<>();
        for (Map.Entry<String, Object> entry : panelsData.entrySet()) {
          if (entry.getValue() instanceof Map) {
            int[] pos = readPosition((Map<String, Object>) entry.getValue());
            if (pos != null) {
              positions.put(entry.getKey(), pos);
            }
          }
        }
        clickGuiModule.setPendingPanels(positions);
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
    if (property instanceof me.friendly.exeter.properties.ActionProperty) {
      return;
    }
    String key = property.getAliases()[0];
    Object value = property.getValue();
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
    File file = configDir.resolve(fileName).toFile();

    try {
      Map<String, Object> data = new HashMap<>();

      // Save module state
      if (module instanceof Toggleable) {
        ToggleableModule toggleable = (ToggleableModule) module;
        Map<String, Object> moduleData = new HashMap<>();
        moduleData.put("enabled", toggleable.isRunning());
        var keybind =
            Exeter.getInstance().getKeybindManager().getKeybindByLabel(toggleable.getLabel());
        moduleData.put("keybind", (long) (keybind != null ? keybind.getKey() : 0));
        moduleData.put("keycodes", "sdl");
        data.put("module", moduleData);
      }

      // Save properties (including children)
      if (!module.getProperties().isEmpty()) {
        Map<String, Object> settings = new HashMap<>();
        for (Property<?> property : module.getProperties()) {
          savePropertyRecursive(property, settings);
        }
        if (!settings.isEmpty()) {
          data.put("settings", settings);
        }
      }

      // Save HUD module position and corner
      if (module instanceof HudModule) {
        HudModule hudModule = (HudModule) module;
        Map<String, Object> hudData = new HashMap<>();
        hudData.put("x", (long) hudModule.getX());
        hudData.put("y", (long) hudModule.getY());
        hudData.put("corner", hudModule.getCorner().name());
        hudData.put("free", hudModule.isFree());
        data.put("hud", hudData);
      }

      // Save window positions
      if (module
          instanceof me.friendly.exeter.module.impl.toggle.client.WindowsModule windowsModule) {
        Map<String, Object> windowsData = new HashMap<>();
        for (Map.Entry<String, int[]> entry : windowsModule.getPendingPositions().entrySet()) {
          Map<String, Object> pos = new HashMap<>();
          pos.put("x", (long) entry.getValue()[0]);
          pos.put("y", (long) entry.getValue()[1]);
          if (windowsModule.getPendingHidden().containsKey(entry.getKey())) {
            pos.put("hidden", windowsModule.getPendingHidden().get(entry.getKey()));
          }
          windowsData.put(entry.getKey(), pos);
        }
        data.put("windows", windowsData);
      }

      // Save ClickGUI panel positions
      if (module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui clickGuiModule) {
        Map<String, Object> panelsData = new HashMap<>();
        for (Map.Entry<String, int[]> entry : clickGuiModule.getPendingPanels().entrySet()) {
          Map<String, Object> pos = new HashMap<>();
          pos.put("x", (long) entry.getValue()[0]);
          pos.put("y", (long) entry.getValue()[1]);
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
}
