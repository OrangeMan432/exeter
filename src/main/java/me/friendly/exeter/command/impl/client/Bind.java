package me.friendly.exeter.command.impl.client;

import me.friendly.api.interfaces.Toggleable;
import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;
import org.lwjgl.input.Keyboard;

public final class Bind extends Command {
  public Bind() {
    super(new String[] {"bind"}, new Argument("module"), new Argument("key"));
    setDescription("Bind a module to a key");
  }

  @Override
  public String dispatch() {
    Module module =
        Exeter.getInstance()
            .getModuleManager()
            .getModuleByAlias(this.getArgument("module").getValue());
    String keyName = this.getArgument("key").getValue().toUpperCase();
    int key = keyNameToKeyCode(keyName);
    if (module == null) {
      return "No such module exists.";
    }
    if (!(module instanceof Toggleable)) {
      return "That module is not toggleable.";
    }
    if (key < 0) {
      return "Unknown key: " + keyName;
    }
    ToggleableModule toggleableModule = (ToggleableModule) module;
    Exeter.getInstance()
        .getKeybindManager()
        .getKeybindByLabel(toggleableModule.getLabel())
        .setKey(key);
    return toggleableModule.getLabel() + " has been bound to " + keyName + ".";
  }

  private int keyNameToKeyCode(String name) {
    if (name.equals("NONE") || name.equals("UNKNOWN")) {
      return 0;
    }
    if (name.length() == 1) {
      char c = name.charAt(0);
      if (c >= 'A' && c <= 'Z') {
        return Keyboard.KEY_A + (c - 'A');
      }
      if (c >= '0' && c <= '9') {
        if (c == '0') return Keyboard.KEY_0;
        return Keyboard.KEY_1 + (c - '1');
      }
      if (c == " ".charAt(0)) return Keyboard.KEY_SPACE;
      if (c == ',') return Keyboard.KEY_COMMA;
      if (c == '.') return Keyboard.KEY_PERIOD;
      if (c == ';') return Keyboard.KEY_SEMICOLON;
      if (c == '\'') return Keyboard.KEY_APOSTROPHE;
      if (c == '-') return Keyboard.KEY_MINUS;
      if (c == '/') return Keyboard.KEY_SLASH;
    }
    if (name.equals("SPACE")) return Keyboard.KEY_SPACE;
    if (name.equals("SHIFT") || name.equals("LSHIFT")) return Keyboard.KEY_LSHIFT;
    if (name.equals("RSHIFT")) return Keyboard.KEY_RSHIFT;
    if (name.equals("CTRL") || name.equals("LCTRL")) return Keyboard.KEY_LCONTROL;
    if (name.equals("RCTRL")) return Keyboard.KEY_RCONTROL;
    if (name.equals("ALT") || name.equals("LALT")) return Keyboard.KEY_LMENU;
    if (name.equals("RALT")) return Keyboard.KEY_RMENU;
    if (name.equals("ESC") || name.equals("ESCAPE")) return Keyboard.KEY_ESCAPE;
    if (name.equals("GRAVE") || name.equals("TILDE") || name.equals("`")) return Keyboard.KEY_GRAVE;
    if (name.equals("TAB")) return Keyboard.KEY_TAB;
    if (name.equals("CAPS")) return Keyboard.KEY_CAPITAL;
    if (name.startsWith("F")) {
      try {
        int f = Integer.parseInt(name.substring(1));
        if (f >= 1 && f <= 10) {
          return Keyboard.KEY_F1 + (f - 1);
        }
        if (f == 11) return Keyboard.KEY_F11;
        if (f == 12) return Keyboard.KEY_F12;
      } catch (NumberFormatException ignored) {
      }
    }
    return -1;
  }
}
