package me.friendly.exeter.module.impl.toggle.render.tabgui.item;

import java.util.ArrayList;
import me.friendly.api.interfaces.Labeled;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.tabgui.GuiTabHandler;

public class GuiTab implements Labeled {
  private final GuiTabHandler gui;
  private ArrayList<GuiItem> mods = new ArrayList<GuiItem>();
  private int menuHeight = 0;
  private int menuWidth = 0;
  private final String label;

  public GuiTab(GuiTabHandler gui, String label) {
    this.gui = gui;
    this.label = label;
  }

  public void drawTabMenu(int x, int y) {
    countMenuSize();
    int boxY = y;
    GuiTabHandler.fillRect(
        x, y, x + menuWidth + 4, y + menuHeight, 0x66000000);
    GuiTabHandler.drawBorder(x, y, x + menuWidth + 4, y + menuHeight, 0xFF000000);
    for (int i = 0; i < mods.size(); ++i) {
      int transitionTop =
          gui.getTransition()
              + (gui.getSelectedItem() == 0 && gui.getTransition() < 0
                  ? -gui.getTransition()
                  : 0);
      int transitionBottom =
          gui.getTransition()
              + (gui.getSelectedItem() == mods.size() - 1 && gui.getTransition() > 0
                  ? -gui.getTransition()
                  : 0);
      if (gui.getSelectedItem() == i) {
        GuiTabHandler.fillRect(
            x, boxY + transitionTop, x + menuWidth + 4, boxY + 12 + transitionBottom, 0x80555555);
      }
      FontUtil.drawString(
          mods.get(i).getToggleableModule().getLabel(),
          (float) (x + 2),
          (float) (y + gui.getTabHeight() * i + 2),
          mods.get(i).getToggleableModule().isRunning() ? 0xFF00FF00 : 0xFFAAAAAA);
      boxY += 12;
    }
  }

  private void countMenuSize() {
    int maxWidth = 0;
    for (GuiItem module : mods) {
      int w =
          FontUtil.getStringWidth(module.getToggleableModule().getAliases()[0]) + 4;
      if (w > maxWidth) maxWidth = w;
    }
    menuWidth = maxWidth;
    menuHeight = mods.size() * gui.getTabHeight();
  }

  @Override
  public String getLabel() {
    return label;
  }

  public ArrayList<GuiItem> getMods() {
    return mods;
  }
}
