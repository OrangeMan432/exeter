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
    CLIENT
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
      cmp =
          Comparator.comparingInt(
                  (Module m) ->
                      me.friendly.api.minecraft.render.font.FontUtil.getStringWidth(
                          getTag(m.getLabel())))
              .reversed();
    }
    modules.sort(cmp);

    List<TextEntry> entries = new ArrayList<>();
    boolean syncClient = colorMode.getValue() == ColorMode.CLIENT;
    for (Module module : modules) {
      if (!(module instanceof ToggleableModule tm)) continue;
      if (!tm.isRunning() || !tm.isDrawn()) continue;
      if (tm.getModuleType() == ModuleType.HUD) continue;
      int color = syncClient ? Colors.getHudMain() : tm.getColor() | 0xFF000000;
      entries.add(new TextEntry(getTag(tm.getLabel()), color));
    }
    return entries;
  }

  private String getTag(String tag) {
    switch (look.getValue()) {
      case UPPER:
        return tag.toUpperCase();
      case LOWER:
        return tag.toLowerCase();
      case CUB:
        return String.format("[%s]", tag.toLowerCase());
      default:
        return tag;
    }
  }
}
