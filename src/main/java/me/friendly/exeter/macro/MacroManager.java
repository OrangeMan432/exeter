package me.friendly.exeter.macro;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.config.Config;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.InputEvent;
import net.minecraft.client.Minecraft;

/** Owns macros: key dispatch, command execution and macros.json persistence. */
public final class MacroManager extends ListRegistry<Macro> {

  private final Config config;

  public MacroManager() {
    this.registry = new ArrayList();
    this.config =
        new Config("macros.json") {
          @Override
          public void load(Object... source) {
            loadMacros();
          }

          @Override
          public void save(Object... destination) {
            saveMacros();
          }
        };
    Exeter.getInstance()
        .getEventManager()
        .register(
            new Listener<InputEvent>("macros_input_listener") {
              @Override
              public void call(InputEvent event) {
                if (event.getType() != InputEvent.Type.KEYBOARD_KEY_PRESS) return;
                var mc = Minecraft.getInstance();
                if (mc.gui != null && mc.gui.screen() != null) return;
                for (Macro macro : getRegistry()) {
                  if (macro.getKey() != 0 && macro.getKey() == event.getKey()) {
                    fire(macro);
                  }
                }
              }
            });
  }

  public Macro getMacro(String name) {
    for (Macro macro : getRegistry()) {
      if (macro.getName().equalsIgnoreCase(name)) return macro;
    }
    return null;
  }

  public boolean add(Macro macro) {
    if (getMacro(macro.getName()) != null) return false;
    getRegistry().add(macro);
    saveMacros();
    return true;
  }

  public boolean remove(String name) {
    Macro macro = getMacro(name);
    if (macro == null) return false;
    if (macro.isEnabled()) {
      runLines(macro.getOnDisable());
    }
    getRegistry().remove(macro);
    saveMacros();
    return true;
  }

  /** Press behavior: instant macros fire, toggle macros flip state. */
  public void fire(Macro macro) {
    if (macro.getMode() == Macro.Mode.INSTANT) {
      runLines(macro.getCommands());
      return;
    }
    setEnabled(macro, !macro.isEnabled());
  }

  public void setEnabled(Macro macro, boolean enabled) {
    if (macro.getMode() != Macro.Mode.TOGGLE || macro.isEnabled() == enabled) return;
    runLines(enabled ? macro.getOnEnable() : macro.getOnDisable());
    macro.setEnabled(enabled);
    saveMacros();
  }

  private void runLines(List<String> lines) {
    var commandManager = Exeter.getInstance().getCommandManager();
    String prefix = commandManager.getPrefix();
    for (String line : lines) {
      if (line == null || line.isBlank()) continue;
      String input = line.trim();
      if (input.startsWith(prefix)) {
        input = input.substring(prefix.length());
      }
      me.friendly.exeter.logging.Logger.getLogger()
          .printToChat(commandManager.dispatchDirect(input));
    }
  }

  public void saveMacros() {
    JsonArray array = new JsonArray();
    for (Macro macro : getRegistry()) {
      JsonObject entry = new JsonObject();
      entry.addProperty("name", macro.getName());
      entry.addProperty("mode", macro.getMode().name());
      entry.addProperty("key", macro.getKey());
      entry.addProperty("enabled", macro.isEnabled());
      JsonArray commands = new JsonArray();
      for (String line : macro.getCommands()) {
        commands.add(line);
      }
      entry.add("commands", commands);
      JsonArray onEnable = new JsonArray();
      for (String line : macro.getOnEnable()) {
        onEnable.add(line);
      }
      entry.add("onEnable", onEnable);
      JsonArray onDisable = new JsonArray();
      for (String line : macro.getOnDisable()) {
        onDisable.add(line);
      }
      entry.add("onDisable", onDisable);
      array.add(entry);
    }
    try {
      if (config.getFile().getParentFile() != null) {
        config.getFile().getParentFile().mkdirs();
      }
      try (FileWriter writer = new FileWriter(config.getFile())) {
        writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(array));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void loadMacros() {
    if (!config.getFile().exists()) {
      return;
    }
    try (FileReader reader = new FileReader(config.getFile())) {
      JsonElement root = new JsonParser().parse(reader);
      if (!(root instanceof JsonArray array)) {
        return;
      }
      getRegistry().clear();
      for (JsonElement node : array) {
        if (!(node instanceof JsonObject entry)) {
          continue;
        }
        try {
          loadEntry(entry);
        } catch (RuntimeException e) {
          e.printStackTrace();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void loadEntry(JsonObject entry) {
    Macro macro = new Macro(entry.get("name").getAsString());
    try {
      macro.setMode(Macro.Mode.valueOf(entry.get("mode").getAsString()));
    } catch (IllegalArgumentException e) {
      macro.setMode(Macro.Mode.INSTANT);
    }
    macro.setKey(entry.get("key").getAsInt());
    for (var e : entry.getAsJsonArray("commands")) {
      macro.getCommands().add(e.getAsString());
    }
    for (var e : entry.getAsJsonArray("onEnable")) {
      macro.getOnEnable().add(e.getAsString());
    }
    for (var e : entry.getAsJsonArray("onDisable")) {
      macro.getOnDisable().add(e.getAsString());
    }
    // Restored silently: no commands run for already-enabled macros at load.
    if (entry.has("enabled") && entry.get("enabled").getAsBoolean()) {
      macro.setEnabled(true);
    }
    getRegistry().add(macro);
  }
}
