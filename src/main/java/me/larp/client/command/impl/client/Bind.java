package me.larp.client.command.impl.client;

import me.larp.api.interfaces.Toggleable;
import me.larp.client.command.Argument;
import me.larp.client.command.Command;
import me.larp.client.core.Larp;
import me.larp.client.module.Module;
import me.larp.client.module.ToggleableModule;
import org.lwjgl.glfw.GLFW;

public final class Bind extends Command {
  public Bind() {
    super(new String[] {"bind"}, new Argument("module"), new Argument("key"));
  }

  @Override
  public String dispatch() {
    Module module =
        Larp.getInstance()
            .getModuleManager()
            .getModuleByAlias(this.getArgument("module").getValue());
    String keyName = this.getArgument("key").getValue().toUpperCase();
    int key = keyNameToGlfwKey(keyName);
    if (module == null) {
      return "No such module exists.";
    }
    if (!(module instanceof Toggleable)) {
      return "That module is not toggleable.";
    }
    if (key == GLFW.GLFW_KEY_UNKNOWN) {
      return "Unknown key: " + keyName;
    }
    ToggleableModule toggleableModule = (ToggleableModule) module;
    Larp.getInstance()
        .getKeybindManager()
        .getKeybindByLabel(toggleableModule.getLabel())
        .setKey(key);
    return String.format("&e%s&7 has been bound to &e%s&7.", toggleableModule.getLabel(), keyName);
  }

  private int keyNameToGlfwKey(String name) {
    if (name.length() == 1) {
      char c = name.charAt(0);
      if (c >= 'A' && c <= 'Z') return c - 'A' + GLFW.GLFW_KEY_A;
      if (c >= '0' && c <= '9') return c - '0' + GLFW.GLFW_KEY_0;
    }
    if (name.equals("F1")) return GLFW.GLFW_KEY_F1;
    if (name.equals("F2")) return GLFW.GLFW_KEY_F2;
    if (name.equals("F3")) return GLFW.GLFW_KEY_F3;
    if (name.equals("F4")) return GLFW.GLFW_KEY_F4;
    if (name.equals("F5")) return GLFW.GLFW_KEY_F5;
    if (name.equals("F6")) return GLFW.GLFW_KEY_F6;
    if (name.equals("F7")) return GLFW.GLFW_KEY_F7;
    if (name.equals("F8")) return GLFW.GLFW_KEY_F8;
    if (name.equals("F9")) return GLFW.GLFW_KEY_F9;
    if (name.equals("F10")) return GLFW.GLFW_KEY_F10;
    if (name.equals("F11")) return GLFW.GLFW_KEY_F11;
    if (name.equals("F12")) return GLFW.GLFW_KEY_F12;
    if (name.equals("LSHIFT")
        || name.equals("LEFTSHIFT")
        || name.equals("LEFT_SHIFT")
        || name.equals("SHIFT")) return GLFW.GLFW_KEY_LEFT_SHIFT;
    if (name.equals("RSHIFT") || name.equals("RIGHTSHIFT") || name.equals("RIGHT_SHIFT"))
      return GLFW.GLFW_KEY_RIGHT_SHIFT;
    if (name.equals("LCONTROL")
        || name.equals("LEFTCONTROL")
        || name.equals("LEFT_CONTROL")
        || name.equals("CONTROL")) return GLFW.GLFW_KEY_LEFT_CONTROL;
    if (name.equals("RCONTROL") || name.equals("RIGHTCONTROL") || name.equals("RIGHT_CONTROL"))
      return GLFW.GLFW_KEY_RIGHT_CONTROL;
    if (name.equals("LALT")
        || name.equals("LEFTALT")
        || name.equals("LEFT_ALT")
        || name.equals("ALT")) return GLFW.GLFW_KEY_LEFT_ALT;
    if (name.equals("RALT") || name.equals("RIGHTALT") || name.equals("RIGHT_ALT"))
      return GLFW.GLFW_KEY_RIGHT_ALT;
    if (name.equals("SPACE")) return GLFW.GLFW_KEY_SPACE;
    if (name.equals("TAB")) return GLFW.GLFW_KEY_TAB;
    if (name.equals("ESCAPE")) return GLFW.GLFW_KEY_ESCAPE;
    if (name.equals("RETURN") || name.equals("ENTER")) return GLFW.GLFW_KEY_ENTER;
    if (name.equals("BACK")) return GLFW.GLFW_KEY_BACKSPACE;
    if (name.equals("DELETE")) return GLFW.GLFW_KEY_DELETE;
    if (name.equals("UP")) return GLFW.GLFW_KEY_UP;
    if (name.equals("DOWN")) return GLFW.GLFW_KEY_DOWN;
    if (name.equals("LEFT")) return GLFW.GLFW_KEY_LEFT;
    if (name.equals("RIGHT")) return GLFW.GLFW_KEY_RIGHT;
    if (name.equals("HOME")) return GLFW.GLFW_KEY_HOME;
    if (name.equals("END")) return GLFW.GLFW_KEY_END;
    if (name.equals("PRIOR") || name.equals("PAGEUP")) return GLFW.GLFW_KEY_PAGE_UP;
    if (name.equals("NEXT") || name.equals("PAGEDOWN")) return GLFW.GLFW_KEY_PAGE_DOWN;
    if (name.equals("INSERT")) return GLFW.GLFW_KEY_INSERT;
    if (name.equals("MINUS")) return GLFW.GLFW_KEY_MINUS;
    if (name.equals("EQUALS")) return GLFW.GLFW_KEY_EQUAL;
    if (name.equals("LBRACKET")) return GLFW.GLFW_KEY_LEFT_BRACKET;
    if (name.equals("RBRACKET")) return GLFW.GLFW_KEY_RIGHT_BRACKET;
    if (name.equals("SEMICOLON")) return GLFW.GLFW_KEY_SEMICOLON;
    if (name.equals("APOSTROPHE")) return GLFW.GLFW_KEY_APOSTROPHE;
    if (name.equals("GRAVE")) return GLFW.GLFW_KEY_GRAVE_ACCENT;
    if (name.equals("BACKSLASH")) return GLFW.GLFW_KEY_BACKSLASH;
    if (name.equals("COMMA")) return GLFW.GLFW_KEY_COMMA;
    if (name.equals("PERIOD")) return GLFW.GLFW_KEY_PERIOD;
    if (name.equals("SLASH")) return GLFW.GLFW_KEY_SLASH;
    if (name.equals("NUMPAD0")) return GLFW.GLFW_KEY_KP_0;
    if (name.equals("NUMPAD1")) return GLFW.GLFW_KEY_KP_1;
    if (name.equals("NUMPAD2")) return GLFW.GLFW_KEY_KP_2;
    if (name.equals("NUMPAD3")) return GLFW.GLFW_KEY_KP_3;
    if (name.equals("NUMPAD4")) return GLFW.GLFW_KEY_KP_4;
    if (name.equals("NUMPAD5")) return GLFW.GLFW_KEY_KP_5;
    if (name.equals("NUMPAD6")) return GLFW.GLFW_KEY_KP_6;
    if (name.equals("NUMPAD7")) return GLFW.GLFW_KEY_KP_7;
    if (name.equals("NUMPAD8")) return GLFW.GLFW_KEY_KP_8;
    if (name.equals("NUMPAD9")) return GLFW.GLFW_KEY_KP_9;
    return GLFW.GLFW_KEY_UNKNOWN;
  }
}
