package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.macro.Macro;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.util.KeyNames;

/** ArrayList-style readout of enabled toggle macros. */
public final class MacroListHud extends ListHudModule {

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

  public MacroListHud() {
    super("MacroList", new String[] {"macrolist", "macros"}, Corner.TOP_RIGHT);
    setDescription("Displays enabled toggle macros in a list.");
    this.offerProperties(organize, look, colorMode);
    organize.setDescription("Sorts entries alphabetically or longest first.");
    look.setDescription("Controls name casing, including an optional bracketed style.");
    colorMode.setDescription("Chooses between the client theme colors.");
  }

  @Override
  protected List<TextEntry> getEntries() {
    List<Macro> macros = new ArrayList<>(Exeter.getInstance().getMacroManager().getRegistry());
    macros.removeIf(macro -> macro.getMode() != Macro.Mode.TOGGLE || !macro.isEnabled());

    Comparator<Macro> cmp;
    if (organize.getValue() == Organize.ABC) {
      cmp = Comparator.comparing(Macro::getName);
    } else {
      cmp = Comparator.comparingInt((Macro m) -> fullWidth(m)).reversed();
    }
    macros.sort(cmp);

    List<TextEntry> entries = new ArrayList<>();
    ColorMode color = colorMode.getValue();
    for (Macro macro : macros) {
      int entryColor =
          color == ColorMode.CLIENT_ACCENT ? Colors.getHudAccent() : Colors.getHudMain();
      String suffix = macro.getKey() == 0 ? "" : " [" + KeyNames.name(macro.getKey()) + "]";
      entries.add(new TextEntry(cased(macro.getName()), entryColor, suffix, 0xFFAAAAAA));
    }
    return entries;
  }

  private int fullWidth(Macro macro) {
    String full = cased(macro.getName());
    if (macro.getKey() != 0) full += " [" + KeyNames.name(macro.getKey()) + "]";
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
