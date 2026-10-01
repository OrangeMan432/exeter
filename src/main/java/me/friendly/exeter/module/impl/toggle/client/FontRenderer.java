package me.friendly.exeter.module.impl.toggle.client;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.Property;

public class FontRenderer extends ToggleableModule {

  public enum Face {
    VANILLA,
    LEXEND_DECA,
    JETBRAINS_MONO,
    SYSTEM
  }

  public final EnumProperty<Face> face = new EnumProperty<>(Face.VANILLA, "Face", "face", "font");
  public final Property<String> family =
      new Property<>("Arial", "Family", "family", "systemfont");

  public FontRenderer() {
    super("Font", new String[] {"font", "fonts", "typeface"}, 0xFFFFFFFF, ModuleType.CLIENT);
    setDescription("TrueType font for client text.");
    offerProperties(face, family);
  }

  public static FontRenderer get() {
    if (Exeter.getInstance() == null) return null;
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("font");
    return module instanceof FontRenderer ? (FontRenderer) module : null;
  }
}
