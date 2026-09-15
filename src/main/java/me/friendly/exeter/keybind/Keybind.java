package me.friendly.exeter.keybind;

import me.friendly.api.interfaces.Labeled;

/** An {@link Object} that represents a keybind, and an implementation of Labeled */
public abstract class Keybind implements Labeled {
  private final String label;
  private int key;

  public Keybind(String label, int key) {
    this.label = label;
    this.key = key;
  }

  @Override
  public String getLabel() {
    return this.label;
  }

  public int getKey() {
    return this.key;
  }

  public void setKey(int key) {
    this.key = key;
  }

  public abstract void onPressed();

  /**
   * Migrates a pre-26.3 GLFW keycode to the SDL keycode used by
   * {@link com.mojang.blaze3d.platform.InputConstants}. Unknown values pass through unchanged.
   */
  public static int migrateLegacyGlfwKey(int glfw) {
    if (glfw >= 'A' && glfw <= 'Z') {
      return glfw - 'A' + com.mojang.blaze3d.platform.InputConstants.KEY_A;
    }
    if (glfw >= 290 && glfw <= 301) {
      return glfw - 290 + com.mojang.blaze3d.platform.InputConstants.KEY_F1;
    }
    switch (glfw) {
      case 0:
        return 0;
      case 32:
        return com.mojang.blaze3d.platform.InputConstants.KEY_SPACE;
      case 39:
        return com.mojang.blaze3d.platform.InputConstants.KEY_APOSTROPHE;
      case 44:
        return com.mojang.blaze3d.platform.InputConstants.KEY_COMMA;
      case 45:
        return com.mojang.blaze3d.platform.InputConstants.KEY_MINUS;
      case 46:
        return com.mojang.blaze3d.platform.InputConstants.KEY_PERIOD;
      case 47:
        return com.mojang.blaze3d.platform.InputConstants.KEY_SLASH;
      case 48:
        return com.mojang.blaze3d.platform.InputConstants.KEY_0;
      case 49:
        return com.mojang.blaze3d.platform.InputConstants.KEY_1;
      case 50:
        return com.mojang.blaze3d.platform.InputConstants.KEY_2;
      case 51:
        return com.mojang.blaze3d.platform.InputConstants.KEY_3;
      case 52:
        return com.mojang.blaze3d.platform.InputConstants.KEY_4;
      case 53:
        return com.mojang.blaze3d.platform.InputConstants.KEY_5;
      case 54:
        return com.mojang.blaze3d.platform.InputConstants.KEY_6;
      case 55:
        return com.mojang.blaze3d.platform.InputConstants.KEY_7;
      case 56:
        return com.mojang.blaze3d.platform.InputConstants.KEY_8;
      case 57:
        return com.mojang.blaze3d.platform.InputConstants.KEY_9;
      case 59:
        return com.mojang.blaze3d.platform.InputConstants.KEY_SEMICOLON;
      case 61:
        return com.mojang.blaze3d.platform.InputConstants.KEY_EQUALS;
      case 91:
        return com.mojang.blaze3d.platform.InputConstants.KEY_LBRACKET;
      case 92:
        return com.mojang.blaze3d.platform.InputConstants.KEY_BACKSLASH;
      case 93:
        return com.mojang.blaze3d.platform.InputConstants.KEY_RBRACKET;
      case 96:
        return com.mojang.blaze3d.platform.InputConstants.KEY_GRAVE;
      case 256:
        return com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE;
      case 257:
        return com.mojang.blaze3d.platform.InputConstants.KEY_RETURN;
      case 258:
        return com.mojang.blaze3d.platform.InputConstants.KEY_TAB;
      case 259:
        return com.mojang.blaze3d.platform.InputConstants.KEY_BACKSPACE;
      case 260:
        return com.mojang.blaze3d.platform.InputConstants.KEY_INSERT;
      case 261:
        return com.mojang.blaze3d.platform.InputConstants.KEY_DELETE;
      case 262:
        return com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT;
      case 263:
        return com.mojang.blaze3d.platform.InputConstants.KEY_LEFT;
      case 264:
        return com.mojang.blaze3d.platform.InputConstants.KEY_DOWN;
      case 265:
        return com.mojang.blaze3d.platform.InputConstants.KEY_UP;
      case 266:
        return com.mojang.blaze3d.platform.InputConstants.KEY_PAGEUP;
      case 267:
        return com.mojang.blaze3d.platform.InputConstants.KEY_PAGEDOWN;
      case 268:
        return com.mojang.blaze3d.platform.InputConstants.KEY_HOME;
      case 269:
        return com.mojang.blaze3d.platform.InputConstants.KEY_END;
      case 320:
        return com.mojang.blaze3d.platform.InputConstants.KEY_NUMPAD0;
      case 321:
        return com.mojang.blaze3d.platform.InputConstants.KEY_NUMPAD1;
      case 322:
        return com.mojang.blaze3d.platform.InputConstants.KEY_NUMPAD2;
      case 323:
        return com.mojang.blaze3d.platform.InputConstants.KEY_NUMPAD3;
      case 324:
        return com.mojang.blaze3d.platform.InputConstants.KEY_NUMPAD4;
      case 325:
        return com.mojang.blaze3d.platform.InputConstants.KEY_NUMPAD5;
      case 326:
        return com.mojang.blaze3d.platform.InputConstants.KEY_NUMPAD6;
      case 327:
        return com.mojang.blaze3d.platform.InputConstants.KEY_NUMPAD7;
      case 328:
        return com.mojang.blaze3d.platform.InputConstants.KEY_NUMPAD8;
      case 329:
        return com.mojang.blaze3d.platform.InputConstants.KEY_NUMPAD9;
      case 340:
        return com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT;
      case 341:
        return com.mojang.blaze3d.platform.InputConstants.KEY_LCONTROL;
      case 342:
        return com.mojang.blaze3d.platform.InputConstants.KEY_LALT;
      case 344:
        return com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT;
      case 345:
        return com.mojang.blaze3d.platform.InputConstants.KEY_RCONTROL;
      case 346:
        return com.mojang.blaze3d.platform.InputConstants.KEY_RALT;
      default:
        return glfw;
    }
  }
}
