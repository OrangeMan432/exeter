package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;

public final class ArrayListHud extends HudModule {

  public ArrayListHud() {
    super("ArrayList", new String[] {"arraylist", "array", "list"}, Corner.TOP_RIGHT);
    offerProperties();
  }

  private List<Module> modules() {
    List<Module> result = new ArrayList<Module>();
    if (Exeter.getInstance() == null) {
      return result;
    }
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      if (module instanceof me.friendly.api.interfaces.Toggleable
          && ((me.friendly.api.interfaces.Toggleable) module).isRunning()
          && !(module instanceof HudModule)) {
        result.add(module);
      }
    }
    for (int i = 0; i < result.size(); i++) {
      for (int j = i + 1; j < result.size(); j++) {
        String a = result.get(i).getLabel();
        String b = result.get(j).getLabel();
        if (FontUtil.getStringWidth(b) > FontUtil.getStringWidth(a)) {
          Module tmp = result.get(i);
          result.set(i, result.get(j));
          result.set(j, tmp);
        }
      }
    }
    return result;
  }

  @Override
  public int getWidth() {
    int width = 0;
    for (Module module : modules()) {
      int w = FontUtil.getStringWidth(module.getLabel());
      if (w > width) {
        width = w;
      }
    }
    return width;
  }

  @Override
  public int getHeight() {
    int count = modules().size();
    if (count == 0) {
      return 0;
    }
    return count * (FontUtil.getFontHeight() + 1) - 1;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    List<Module> modules = modules();
    int line = FontUtil.getFontHeight() + 1;
    boolean top = getCorner() == Corner.TOP_LEFT || getCorner() == Corner.TOP_RIGHT;
    boolean right = getCorner() == Corner.TOP_RIGHT || getCorner() == Corner.BOTTOM_RIGHT;
    int y = top ? getY() : getY() + getHeight() - FontUtil.getFontHeight();
    for (int i = 0; i < modules.size(); i++) {
      Module module = top ? modules.get(i) : modules.get(modules.size() - 1 - i);
      int w = FontUtil.getStringWidth(module.getLabel());
      int x = getX();
      if (right) {
        x = getX() + getWidth() - w;
      }
      int color = 0xFFFFFFFF;
      if (module instanceof me.friendly.exeter.module.ToggleableModule) {
        color = ((me.friendly.exeter.module.ToggleableModule) module).getColor();
        if (color == 0) {
          color = 0xFFFFFFFF;
        }
      }
      FontUtil.drawString(module.getLabel(), (float) x, (float) y, color);
      y += top ? line : -line;
    }
  }
}
