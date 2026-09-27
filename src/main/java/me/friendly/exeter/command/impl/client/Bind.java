package me.friendly.exeter.command.impl.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.friendly.api.interfaces.Toggleable;
import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ToggleableModule;

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
    if (key == InputConstants.UNKNOWN.getValue()) {
      return "Unknown key: " + keyName;
    }
    ToggleableModule toggleableModule = (ToggleableModule) module;
    Exeter.getInstance()
        .getKeybindManager()
        .getKeybindByLabel(toggleableModule.getLabel())
        .setKey(key);
    return String.format("&e%s&7 has been bound to &e%s&7.", toggleableModule.getLabel(), keyName);
  }

  private int keyNameToKeyCode(String name) {
    if (name.length() == 1) {
      char c = name.charAt(0);
      if (c >= 'A' && c <= 'Z') return c - 'A' + InputConstants.KEY_A;
      switch (c) {
        case '0':
          return InputConstants.KEY_0;
        case '1':
          return InputConstants.KEY_1;
        case '2':
          return InputConstants.KEY_2;
        case '3':
          return InputConstants.KEY_3;
        case '4':
          return InputConstants.KEY_4;
        case '5':
          return InputConstants.KEY_5;
        case '6':
          return InputConstants.KEY_6;
        case '7':
          return InputConstants.KEY_7;
        case '8':
          return InputConstants.KEY_8;
        case '9':
          return InputConstants.KEY_9;
        default:
          break;
      }
    }
    if (name.equals("F1")) return InputConstants.KEY_F1;
    if (name.equals("F2")) return InputConstants.KEY_F2;
    if (name.equals("F3")) return InputConstants.KEY_F3;
    if (name.equals("F4")) return InputConstants.KEY_F4;
    if (name.equals("F5")) return InputConstants.KEY_F5;
    if (name.equals("F6")) return InputConstants.KEY_F6;
    if (name.equals("F7")) return InputConstants.KEY_F7;
    if (name.equals("F8")) return InputConstants.KEY_F8;
    if (name.equals("F9")) return InputConstants.KEY_F9;
    if (name.equals("F10")) return InputConstants.KEY_F10;
    if (name.equals("F11")) return InputConstants.KEY_F11;
    if (name.equals("F12")) return InputConstants.KEY_F12;
    if (name.equals("LSHIFT")
        || name.equals("LEFTSHIFT")
        || name.equals("LEFT_SHIFT")
        || name.equals("SHIFT")) return InputConstants.KEY_LSHIFT;
    if (name.equals("RSHIFT") || name.equals("RIGHTSHIFT") || name.equals("RIGHT_SHIFT"))
      return InputConstants.KEY_RSHIFT;
    if (name.equals("LCONTROL")
        || name.equals("LEFTCONTROL")
        || name.equals("LEFT_CONTROL")
        || name.equals("CONTROL")) return InputConstants.KEY_LCONTROL;
    if (name.equals("RCONTROL") || name.equals("RIGHTCONTROL") || name.equals("RIGHT_CONTROL"))
      return InputConstants.KEY_RCONTROL;
    if (name.equals("LALT")
        || name.equals("LEFTALT")
        || name.equals("LEFT_ALT")
        || name.equals("ALT")) return InputConstants.KEY_LALT;
    if (name.equals("RALT") || name.equals("RIGHTALT") || name.equals("RIGHT_ALT"))
      return InputConstants.KEY_RALT;
    if (name.equals("SPACE")) return InputConstants.KEY_SPACE;
    if (name.equals("TAB")) return InputConstants.KEY_TAB;
    if (name.equals("ESCAPE")) return InputConstants.KEY_ESCAPE;
    if (name.equals("RETURN") || name.equals("ENTER")) return InputConstants.KEY_RETURN;
    if (name.equals("BACK")) return InputConstants.KEY_BACKSPACE;
    if (name.equals("DELETE")) return InputConstants.KEY_DELETE;
    if (name.equals("UP")) return InputConstants.KEY_UP;
    if (name.equals("DOWN")) return InputConstants.KEY_DOWN;
    if (name.equals("LEFT")) return InputConstants.KEY_LEFT;
    if (name.equals("RIGHT")) return InputConstants.KEY_RIGHT;
    if (name.equals("HOME")) return InputConstants.KEY_HOME;
    if (name.equals("END")) return InputConstants.KEY_END;
    if (name.equals("PRIOR") || name.equals("PAGEUP")) return InputConstants.KEY_PAGEUP;
    if (name.equals("NEXT") || name.equals("PAGEDOWN")) return InputConstants.KEY_PAGEDOWN;
    if (name.equals("INSERT")) return InputConstants.KEY_INSERT;
    if (name.equals("MINUS")) return InputConstants.KEY_MINUS;
    if (name.equals("EQUALS")) return InputConstants.KEY_EQUALS;
    if (name.equals("LBRACKET")) return InputConstants.KEY_LBRACKET;
    if (name.equals("RBRACKET")) return InputConstants.KEY_RBRACKET;
    if (name.equals("SEMICOLON")) return InputConstants.KEY_SEMICOLON;
    if (name.equals("APOSTROPHE")) return InputConstants.KEY_APOSTROPHE;
    if (name.equals("GRAVE")) return InputConstants.KEY_GRAVE;
    if (name.equals("BACKSLASH")) return InputConstants.KEY_BACKSLASH;
    if (name.equals("COMMA")) return InputConstants.KEY_COMMA;
    if (name.equals("PERIOD")) return InputConstants.KEY_PERIOD;
    if (name.equals("SLASH")) return InputConstants.KEY_SLASH;
    if (name.equals("NUMPAD0")) return InputConstants.KEY_NUMPAD0;
    if (name.equals("NUMPAD1")) return InputConstants.KEY_NUMPAD1;
    if (name.equals("NUMPAD2")) return InputConstants.KEY_NUMPAD2;
    if (name.equals("NUMPAD3")) return InputConstants.KEY_NUMPAD3;
    if (name.equals("NUMPAD4")) return InputConstants.KEY_NUMPAD4;
    if (name.equals("NUMPAD5")) return InputConstants.KEY_NUMPAD5;
    if (name.equals("NUMPAD6")) return InputConstants.KEY_NUMPAD6;
    if (name.equals("NUMPAD7")) return InputConstants.KEY_NUMPAD7;
    if (name.equals("NUMPAD8")) return InputConstants.KEY_NUMPAD8;
    if (name.equals("NUMPAD9")) return InputConstants.KEY_NUMPAD9;
    return InputConstants.UNKNOWN.getValue();
  }
}
