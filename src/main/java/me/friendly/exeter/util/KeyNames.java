package me.friendly.exeter.util;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/** Display names for SDL keycodes (the ClickGUI bind readout shares this). */
public final class KeyNames {
  private static final Map<Integer, String> NAMES = new HashMap<>();

  static {
    for (Field field : InputConstants.class.getDeclaredFields()) {
      if (!field.getName().startsWith("KEY_")) continue;
      if (!Modifier.isStatic(field.getModifiers()) || field.getType() != int.class) continue;
      try {
        NAMES.putIfAbsent(field.getInt(null), prettify(field.getName().substring(4)));
      } catch (IllegalAccessException e) {
        // Ignore, fall back to the numeric form below.
      }
    }
  }

  private KeyNames() {}

  public static String name(int key) {
    if (key == 0) return "unbound";
    String name = NAMES.get(key);
    if (name != null) return name;
    return "KEY_" + key;
  }

  private static String prettify(String raw) {
    if (raw.length() == 1) return raw;
    String lower = raw.toLowerCase().replace('_', ' ');
    StringBuilder out = new StringBuilder();
    for (String word : lower.split(" ")) {
      if (word.isEmpty()) continue;
      out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
    }
    return out.toString().trim();
  }
}
