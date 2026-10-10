package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.macro.Macro;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.util.KeyNames;

/** Every bound module and macro with its key in gray brackets, ArrayList-style. */
public final class BindListHud extends ListHudModule {

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

  private static final int GRAY = 0xFFAAAAAA;

  public final EnumProperty<Organize> organize =
      new EnumProperty<>(Organize.LENGTH, "Organize", "o");
  public final EnumProperty<Look> look = new EnumProperty<>(Look.DEFAULT, "Casing", "c");
  public final EnumProperty<ColorMode> colorMode =
      new EnumProperty<>(ColorMode.DEFAULT, "Color", "color");

  public BindListHud() {
    super("BindList", new String[] {"bindlist", "binds"}, Corner.TOP_RIGHT);
    setDescription("Displays bound modules and macros with their keys.");
    this.offerProperties(organize, look, colorMode);
    organize.setDescription("Sorts entries alphabetically or longest first.");
    look.setDescription("Controls name casing, including an optional bracketed style.");
    colorMode.setDescription("Chooses between per-module colors or the client theme.");
  }

  private record Row(String name, String key, int color) {
    String full() {
      return name + " [" + key + "]";
    }
  }

  @Override
  protected List<TextEntry> getEntries() {
    List<Row> rows = new ArrayList<>();
    ColorMode color = colorMode.getValue();

    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      if (!(module instanceof ToggleableModule tm)) continue;
      if (tm.getModuleType() == ModuleType.HUD) continue;
      var keybind = Exeter.getInstance().getKeybindManager().getKeybindByLabel(tm.getLabel());
      if (keybind == null || keybind.getKey() == 0) continue;
      int entryColor = tm.getColor() | 0xFF000000;
      if (color == ColorMode.CLIENT) entryColor = Colors.getHudMain();
      else if (color == ColorMode.CLIENT_ACCENT) entryColor = Colors.getHudAccent();
      rows.add(new Row(tm.getLabel(), KeyNames.name(keybind.getKey()), entryColor));
    }

    for (Macro macro : Exeter.getInstance().getMacroManager().getRegistry()) {
      if (macro.getKey() == 0) continue;
      int entryColor =
          color == ColorMode.CLIENT_ACCENT ? Colors.getHudAccent() : Colors.getHudMain();
      rows.add(new Row(macro.getName(), KeyNames.name(macro.getKey()), entryColor));
    }

    Comparator<Row> cmp;
    if (organize.getValue() == Organize.ABC) {
      cmp = Comparator.comparing(Row::name);
    } else {
      cmp =
          Comparator.comparingInt(
                  (Row r) ->
                      me.friendly.api.minecraft.render.font.FontUtil.getStringWidth(
                          cased(r.name()) + " [" + r.key() + "]"))
              .reversed();
    }
    rows.sort(cmp);

    List<TextEntry> entries = new ArrayList<>();
    for (Row row : rows) {
      entries.add(new TextEntry(cased(row.name()), row.color(), " [" + row.key() + "]", GRAY));
    }
    return entries;
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
