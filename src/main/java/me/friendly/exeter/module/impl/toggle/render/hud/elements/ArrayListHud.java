package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.properties.EnumProperty;

public final class ArrayListHud extends HudModule {

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

  public final EnumProperty<Organize> organize =
      new EnumProperty<>(Organize.LENGTH, "Organize", "o");
  public final EnumProperty<Look> look = new EnumProperty<>(Look.DEFAULT, "Casing", "c");

  public ArrayListHud() {
    super("ArrayList", new String[] {"arraylist", "array", "al"}, Corner.TOP_RIGHT);
    setDescription("Displays active modules in a list.");
    this.offerProperties(organize, look);
  }

  @Override
  public int getWidth() {
    int max = 0;
    for (String line : getLines()) {
      int w = FontUtil.getStringWidth(line);
      if (w > max) max = w;
    }
    return max;
  }

  @Override
  public int getHeight() {
    return getLines().size() * 9;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    List<Module> modules =
        new ArrayList<>(Exeter.getInstance().getModuleManager().getRegistry());
    if (organize.getValue() == Organize.ABC) {
      modules.sort((a, b) -> a.getLabel().compareTo(b.getLabel()));
    } else {
      modules.sort((a, b) -> FontUtil.getStringWidth(b.getLabel()) - FontUtil.getStringWidth(a.getLabel()));
    }
    boolean bottom = getY() > scaledHeight / 2;
    if (bottom) Collections.reverse(modules);

    int py = getY();
    for (Module module : modules) {
      if (!(module instanceof ToggleableModule tm)) continue;
      if (!tm.isRunning() || !tm.isDrawn()) continue;
      if (tm.getModuleType() == ModuleType.HUD) continue;
      int color = tm.getColor() | 0xFF000000;
      String label = getTag(tm.getLabel());
      FontUtil.drawString(label, getX(), py, color);
      py += bottom ? -9 : 9;
    }
  }

  private List<String> getLines() {
    List<String> lines = new ArrayList<>();
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      if (module instanceof ToggleableModule tm && tm.isRunning() && tm.isDrawn()
          && tm.getModuleType() != ModuleType.HUD) {
        lines.add(getTag(tm.getLabel()));
      }
    }
    return lines;
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
