package me.friendly.exeter.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
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

      // Load HUD module position and corner.
      if (module instanceof me.friendly.exeter.module.impl.toggle.render.hud.HudModule
          && data.has("hud")) {
        me.friendly.exeter.module.impl.toggle.render.hud.HudModule hudModule =
            (me.friendly.exeter.module.impl.toggle.render.hud.HudModule) module;
        JsonObject hudData = data.getAsJsonObject("hud");
        if (hudData.has("x")) {
          hudModule.setX(hudData.get("x").getAsInt());
        }
        if (hudData.has("y")) {
          hudModule.setY(hudData.get("y").getAsInt());
        }
        if (hudData.has("corner")) {
          try {
            hudModule.setCorner(
                me.friendly.exeter.module.impl.toggle.render.hud.HudModule.Corner.valueOf(
                    hudData.get("corner").getAsString()));
          } catch (IllegalArgumentException ignored) {
          }
        }
      }

      if (module
              instanceof me.friendly.exeter.module.impl.toggle.client.WindowsModule
          && data.has("windows")) {        JsonObject windowsData = data.getAsJsonObject("windows");
        Map<String, int[]> positions = new HashMap<String, int[]>();
        for (Map.Entry<String, JsonElement> entry : windowsData.entrySet()) {
          int[] pos = readPosition(entry.getValue());
          if (pos != null) {
            positions.put(entry.getKey(), pos);
          }
        }
        ((me.friendly.exeter.module.impl.toggle.client.WindowsModule) module)
            .setPendingPositions(positions);
      }

      if (module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui
          && data.has("panels")) {
        JsonObject panelsData = data.getAsJsonObject("panels");
        Map<String, int[]> positions = new HashMap<String, int[]>();
        for (Map.Entry<String, JsonElement> entry : panelsData.entrySet()) {
          int[] pos = readPosition(entry.getValue());
          if (pos != null) {
            positions.put(entry.getKey(), pos);
          }
        }
        ((me.friendly.exeter.module.impl.toggle.render.ClickGui) module)
            .setPendingPanels(positions);
      }
    } catch (Exception e) {
      System.err.println(
          "[Exeter] Failed to load config for " + module.getLabel() + ": " + e.getMessage());
    }
  }

  private int[] readPosition(JsonElement element) {
    try {
      if (element != null && element.isJsonObject()) {
        JsonObject table = element.getAsJsonObject();
        if (table.has("x") && table.has("y")) {
          return new int[] {table.get("x").getAsInt(), table.get("y").getAsInt()};
        }
      }
    } catch (Exception ignored) {
    }
    return null;
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

      if (module instanceof me.friendly.exeter.module.impl.toggle.client.WindowsModule) {
        Map<String, int[]> positions =
            ((me.friendly.exeter.module.impl.toggle.client.WindowsModule) module)
                .getPendingPositions();
        JsonObject windowsData = new JsonObject();
        for (Map.Entry<String, int[]> entry : positions.entrySet()) {
          JsonObject pos = new JsonObject();
          pos.addProperty("x", entry.getValue()[0]);
          pos.addProperty("y", entry.getValue()[1]);
          windowsData.add(entry.getKey(), pos);
        }
        data.add("windows", windowsData);
      }

      if (module instanceof me.friendly.exeter.module.impl.toggle.render.hud.HudModule) {
        me.friendly.exeter.module.impl.toggle.render.hud.HudModule hudModule =
            (me.friendly.exeter.module.impl.toggle.render.hud.HudModule) module;
        JsonObject hudData = new JsonObject();
        hudData.addProperty("x", hudModule.getX());
        hudData.addProperty("y", hudModule.getY());
        hudData.addProperty("corner", hudModule.getCorner().name());
        data.add("hud", hudData);
      }

      if (module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui) {
        Map<String, int[]> positions =
            ((me.friendly.exeter.module.impl.toggle.render.ClickGui) module).getPendingPanels();
        JsonObject panelsData = new JsonObject();
        for (Map.Entry<String, int[]> entry : positions.entrySet()) {
          JsonObject pos = new JsonObject();
          pos.addProperty("x", entry.getValue()[0]);
          pos.addProperty("y", entry.getValue()[1]);
          panelsData.add(entry.getKey(), pos);
        }
        data.add("panels", panelsData);
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
