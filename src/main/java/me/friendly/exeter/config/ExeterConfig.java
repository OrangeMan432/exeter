package me.friendly.exeter.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Map;
import me.friendly.api.interfaces.Toggleable;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.keybind.Keybind;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.ActionProperty;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;

/**
 * Module settings persistence as Gson JSON. Each module gets its own file:
 *
 * <pre>
 * {
 *   "module": { "enabled": true, "keybind": 19 },
 *   "settings": { "Range": 4.5, "Mode": "FAST" }
 * }
 * </pre>
 */
public class ExeterConfig {
  private static ExeterConfig instance;
  private final File configDir;
  private final Gson gson;

  public ExeterConfig() {
    instance = this;
    this.configDir = new File(Exeter.getInstance().getDirectory(), "modules");
    if (!this.configDir.exists()) {
      this.configDir.mkdirs();
    }
    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  public static ExeterConfig getInstance() {
    return instance;
  }

  /** Loads all module configurations from JSON files. */
  public void loadAll() {
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      loadModule(module);
    }
  }

  /** Saves all module configurations to JSON files. */
  public void saveAll() {
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      saveModule(module);
    }
  }

  /** Loads a single module's configuration from its JSON file. */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void loadModule(Module module) {
    File file = fileFor(module);
    if (!file.exists()) {
      return;
    }
    try {
      FileReader reader = new FileReader(file);
      JsonElement root;
      try {
        root = JsonParser.parseReader(reader);
      } finally {
        reader.close();
      }
      if (root == null || !root.isJsonObject()) {
        return;
      }
      JsonObject data = root.getAsJsonObject();

      boolean restoreEnabled = false;
      boolean hasEnabledState = false;
      if (module instanceof Toggleable && data.has("module")) {
        JsonObject moduleData = data.getAsJsonObject("module");
        if (moduleData.has("enabled")) {
          restoreEnabled = moduleData.get("enabled").getAsBoolean();
          hasEnabledState = true;
        }
        if (moduleData.has("keybind")) {
          int keybind = moduleData.get("keybind").getAsInt();
          Keybind kb =
              Exeter.getInstance().getKeybindManager().getKeybindByLabel(module.getLabel());
          if (kb != null && keybind != 0) {
            kb.setKey(keybind);
          }
        }
      }

      if (data.has("settings")) {
        JsonObject settings = data.getAsJsonObject("settings");
        for (Property<?> property : module.getProperties()) {
          if (property instanceof ActionProperty || property instanceof PopupProperty) {
            continue;
          }
          String key = property.getAliases()[0];
          if (settings.has(key)) {
            applyPropertyValue(property, settings.get(key));
          }
        }
      }

      if (hasEnabledState && module instanceof ToggleableModule) {
        ((ToggleableModule) module).setRunning(restoreEnabled);
      }
    } catch (Exception e) {
      System.err.println(
          "[Exeter] Failed to load config for " + module.getLabel() + ": " + e.getMessage());
    }
  }

  /** Saves a single module's configuration to its JSON file. */
  public void saveModule(Module module) {
    try {
      JsonObject data = new JsonObject();

      if (module instanceof Toggleable) {
        ToggleableModule toggleable = (ToggleableModule) module;
        JsonObject moduleData = new JsonObject();
        moduleData.addProperty("enabled", toggleable.isRunning());
        Keybind kb =
            Exeter.getInstance().getKeybindManager().getKeybindByLabel(toggleable.getLabel());
        moduleData.addProperty("keybind", kb != null ? kb.getKey() : 0);
        data.add("module", moduleData);
      }

      JsonObject settings = new JsonObject();
      for (Property<?> property : module.getProperties()) {
        if (property instanceof ActionProperty || property instanceof PopupProperty) {
          continue;
        }
        saveProperty(property, settings);
      }
      if (settings.size() > 0) {
        data.add("settings", settings);
      }

      FileWriter writer = new FileWriter(fileFor(module));
      try {
        writer.write(gson.toJson(data));
      } finally {
        writer.close();
      }
    } catch (Exception e) {
      System.err.println(
          "[Exeter] Failed to save config for " + module.getLabel() + ": " + e.getMessage());
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void saveProperty(Property<?> property, JsonObject settings) {
    String key = property.getAliases()[0];
    Object value = property.getValue();
    if (value instanceof Enum) {
      settings.addProperty(key, ((Enum<?>) value).name());
    } else if (value instanceof Number) {
      settings.addProperty(key, (Number) value);
    } else if (value instanceof Boolean) {
      settings.addProperty(key, ((Boolean) value).booleanValue());
    } else if (value instanceof String) {
      settings.addProperty(key, (String) value);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void applyPropertyValue(Property<?> property, JsonElement value) {
    if (value == null || value.isJsonNull()) {
      return;
    }
    try {
      if (property instanceof EnumProperty) {
        ((EnumProperty) property).setValue(value.getAsString());
      } else if (property instanceof NumberProperty) {
        Object current = property.getValue();
        if (current instanceof Integer) {
          ((NumberProperty) property).setValue(Integer.valueOf(value.getAsInt()));
        } else if (current instanceof Float) {
          ((NumberProperty) property).setValue(Float.valueOf(value.getAsFloat()));
        } else if (current instanceof Double) {
          ((NumberProperty) property).setValue(Double.valueOf(value.getAsDouble()));
        } else if (current instanceof Long) {
          ((NumberProperty) property).setValue(Long.valueOf(value.getAsLong()));
        }
      } else if (property.getValue() instanceof Boolean) {
        ((Property<Boolean>) property).setValue(Boolean.valueOf(value.getAsBoolean()));
      } else if (property.getValue() instanceof String) {
        ((Property<Object>) property).setValue(value.getAsString());
      }
    } catch (Exception e) {
      System.err.println(
          "[Exeter] Bad value for " + property.getAliases()[0] + ": " + e.getMessage());
    }
  }

  private File fileFor(Module module) {
    return new File(configDir, module.getLabel().toLowerCase().replaceAll(" ", "") + ".json");
  }
}
