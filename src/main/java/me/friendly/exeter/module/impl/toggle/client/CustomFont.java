package me.friendly.exeter.module.impl.toggle.client;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;

public class CustomFont extends ToggleableModule {

  public enum Face {
    VANILLA,
    LEXEND_DECA,
    JETBRAINS_MONO,
    SYSTEM
  }

  public final EnumProperty<Face> face = new EnumProperty<>(Face.VANILLA, "Face", "face", "font");
  public final Property<String> family = new Property<>("Arial", "Family", "family", "systemfont");
  public final NumberProperty<Integer> size =
      new NumberProperty<>(9, 8, 24, "Size", "size", "fontsize");

  public CustomFont() {
    super(
        "CustomFont",
        new String[] {"customfont", "font", "fonts", "typeface"},
        0xFFFFFFFF,
        ModuleType.CLIENT);
    setDescription("TrueType font for client text.");
    offerProperties(face, family, size);
  }

  public static CustomFont get() {
    if (Exeter.getInstance() == null) return null;
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("font");
    return module instanceof CustomFont ? (CustomFont) module : null;
  }
}
