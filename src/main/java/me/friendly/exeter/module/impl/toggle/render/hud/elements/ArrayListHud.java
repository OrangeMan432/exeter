package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.properties.EnumProperty;

public final class ArrayListHud extends ListHudModule {

  public enum Organize {
    ABC,
    LENGTH
  }

  public enum Look {
    DEFAULT,
    LOWER,
    UPPER,
    CUB
  }

  public enum ColorMode {
    DEFAULT,
    CLIENT,
    CLIENT_ACCENT
  }

  public final EnumProperty<Organize> organize =
      new EnumProperty<>(Organize.LENGTH, "Organize", "o");
  public final EnumProperty<Look> look = new EnumProperty<>(Look.DEFAULT, "Casing", "c");
  public final EnumProperty<ColorMode> colorMode =
      new EnumProperty<>(ColorMode.DEFAULT, "Color", "color");

  public ArrayListHud() {
    super("ArrayList", new String[] {"arraylist", "array", "al"}, Corner.TOP_RIGHT);
    setDescription("Displays active modules in a list.");
    this.offerProperties(organize, look, colorMode);
  }

  @Override
  protected List<TextEntry> getEntries() {
    List<Module> modules = new ArrayList<>(Exeter.getInstance().getModuleManager().getRegistry());

    Comparator<Module> cmp;
    if (organize.getValue() == Organize.ABC) {
      cmp = Comparator.comparing(Module::getLabel);
    } else {
      cmp = Comparator.comparingInt((Module m) -> fullWidth(m)).reversed();
    }
    modules.sort(cmp);

    List<TextEntry> entries = new ArrayList<>();
    ColorMode color = colorMode.getValue();
    for (Module module : modules) {
      if (!(module instanceof ToggleableModule tm)) continue;
      if (!tm.isRunning() || !tm.isDrawn()) continue;
      if (tm.getModuleType() == ModuleType.HUD) continue;
      int entryColor = tm.getColor() | 0xFF000000;
      if (color == ColorMode.CLIENT) entryColor = Colors.getHudMain();
      else if (color == ColorMode.CLIENT_ACCENT) entryColor = Colors.getHudAccent();
      entries.add(new TextEntry(cased(tm.getLabel()), entryColor, suffixOf(tm), 0xFFAAAAAA));
    }
    return entries;
  }

  /** Gray bracket info from the module's live tag, or empty when it has none. */
  private static String suffixOf(ToggleableModule module) {
    String tag = module.getTag();
    if (tag == null || tag.isEmpty() || tag.equals(module.getLabel())) return "";
    return " [" + tag + "]";
  }

  private int fullWidth(Module module) {
    String full = cased(module.getLabel());
    if (module instanceof ToggleableModule tm) full += suffixOf(tm);
    return me.friendly.api.minecraft.render.font.FontUtil.getStringWidth(full);
  }

  private String cased(String label) {
    switch (look.getValue()) {
      case UPPER:
        return label.toUpperCase();
      case LOWER:
        return label.toLowerCase();
      case CUB:
        return String.format("[%s]", label.toLowerCase());
      default:
        return label;
    }
  }
}
