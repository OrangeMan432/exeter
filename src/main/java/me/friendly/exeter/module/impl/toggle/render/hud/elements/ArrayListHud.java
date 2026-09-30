package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;

public final class ArrayListHud extends ListHudModule {

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
  protected List<TextEntry> getEntries() {
    List<TextEntry> entries = new ArrayList<TextEntry>();
    for (Module module : modules()) {
      int color = 0xFFFFFFFF;
      if (module instanceof me.friendly.exeter.module.ToggleableModule) {
        color = ((me.friendly.exeter.module.ToggleableModule) module).getColor();
        if (color == 0) {
          color = 0xFFFFFFFF;
        }
      }
      entries.add(new TextEntry(module.getLabel(), color));
    }
    return entries;
  }
}
