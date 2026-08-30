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
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      loadModule(module);
    }
  }

  /** Saves all module configurations to TOML files. */
  public void saveAll() {
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      saveModule(module);
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

      // Load module state (enabled, drawn, keybind)
      if (module instanceof Toggleable && data.containsKey("module")) {
        Map<String, Object> moduleData = (Map<String, Object>) data.get("module");
        ToggleableModule toggleable = (ToggleableModule) module;

        if (moduleData.containsKey("enabled")) {
          boolean enabled = (boolean) moduleData.get("enabled");
          toggleable.setRunning(enabled);
        }

        if (moduleData.containsKey("drawn")) {
          toggleable.setDrawn((boolean) moduleData.get("drawn"));
        }

        if (moduleData.containsKey("keybind")) {
          int keybind = ((Long) moduleData.get("keybind")).intValue();
          Exeter.getInstance()
              .getKeybindManager()
              .getKeybindByLabel(toggleable.getLabel())
              .setKey(keybind);
        }
      }

      // Load properties
      if (data.containsKey("settings")) {
        Map<String, Object> settings = (Map<String, Object>) data.get("settings");
        for (Property<?> property : module.getProperties()) {
          String key = property.getAliases()[0];
          if (settings.containsKey(key)) {
            Object value = settings.get(key);
            applyPropertyValue(property, value);
          }
        }
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
        ((NumberProperty) property).setValue(((Long) value).intValue());
      } else if (property.getValue() instanceof Float) {
        ((NumberProperty) property).setValue(((Double) value).floatValue());
      } else if (property.getValue() instanceof Double) {
        ((NumberProperty) property).setValue(((Number) value).doubleValue());
      } else if (property.getValue() instanceof Long) {
        ((NumberProperty) property).setValue(((Long) value).longValue());
      }
    } else if (property.getValue() instanceof Boolean) {
      ((Property<Boolean>) property).setValue((Boolean) value);
    } else if (property.getValue() instanceof String) {
      ((Property<Object>) property).setValue(value);
    }
  }

  /** Saves a single module's configuration to its TOML file. */
  public void saveModule(Module module) {
    String fileName = module.getLabel().toLowerCase().replaceAll(" ", "") + ".toml";
    File file = configDir.resolve(fileName).toFile();

    Map<String, Object> data = new HashMap<>();

    // Save module state
    if (module instanceof Toggleable) {
      ToggleableModule toggleable = (ToggleableModule) module;
      Map<String, Object> moduleData = new HashMap<>();
      moduleData.put("enabled", toggleable.isRunning());
      moduleData.put("drawn", toggleable.isDrawn());
      moduleData.put(
          "keybind",
          (long)
              Exeter.getInstance()
                  .getKeybindManager()
                  .getKeybindByLabel(toggleable.getLabel())
                  .getKey());
      data.put("module", moduleData);
    }

    // Save properties
    if (!module.getProperties().isEmpty()) {
      Map<String, Object> settings = new HashMap<>();
      for (Property<?> property : module.getProperties()) {
        if (property.getAliases()[0].equals("Drawn")) {
          continue;
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
      }
      if (!settings.isEmpty()) {
        data.put("settings", settings);
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
      }
    }

    try {
      tomlWriter.write(data, file);
    } catch (Exception e) {
      System.err.println(
          "[Exeter] Failed to save config for " + module.getLabel() + ": " + e.getMessage());
    }
  }
}
