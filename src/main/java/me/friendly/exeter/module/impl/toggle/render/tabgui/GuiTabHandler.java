package me.friendly.exeter.module.impl.toggle.render.tabgui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.interfaces.Toggleable;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.tabgui.item.GuiItem;
import me.friendly.exeter.module.impl.toggle.render.tabgui.item.GuiTab;
import net.minecraft.client.render.Tessellator;
import org.lwjgl.opengl.GL11;

public final class GuiTabHandler {
  private int guiHeight = 0;
  public boolean mainMenu = true;
  public int selectedItem = 0;
  public int selectedTab = 0;
  private int tabHeight = 12;
  public final ArrayList<GuiTab> tabs = new ArrayList<GuiTab>();
  public int transition = 0;
  public boolean visible = true;

  public GuiTabHandler() {}

  public void ensureTabsPopulated() {
    if (!tabs.isEmpty()) return;
    List<Module> modules =
        new ArrayList<Module>(Exeter.getInstance().getModuleManager().getRegistry());
    Collections.sort(
        modules,
        new Comparator<Module>() {
          @Override
          public int compare(Module a, Module b) {
            return a.getLabel().compareTo(b.getLabel());
          }
        });
    for (ModuleType moduleType : ModuleType.values()) {
      if (moduleType == ModuleType.HUD) continue;
      GuiTab guiTab = new GuiTab(this, moduleType.getLabel());
      for (Module module : modules) {
        if (module instanceof Toggleable) {
          ToggleableModule toggle = (ToggleableModule) module;
          if (toggle.getModuleType() == moduleType && !toggle.getLabel().equals("ClickGui")) {
            guiTab.getMods().add(new GuiItem(toggle));
          }
        }
      }
      tabs.add(guiTab);
    }
    Collections.sort(
        tabs,
        new Comparator<GuiTab>() {
          @Override
          public int compare(GuiTab a, GuiTab b) {
            return a.getLabel().compareTo(b.getLabel());
          }
        });
    guiHeight = tabs.size() * tabHeight;
  }

  public static void fillRect(int x, int y, int x1, int y1, int color) {
    float a = ((color >> 24) & 0xFF) / 255.0F;
    float r = ((color >> 16) & 0xFF) / 255.0F;
    float g = ((color >> 8) & 0xFF) / 255.0F;
    float b = (color & 0xFF) / 255.0F;
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(r, g, b, a);
    Tessellator tess = Tessellator.INSTANCE;
    tess.start(7);
    tess.vertex((double) x, (double) y1, 0.0);
    tess.vertex((double) x1, (double) y1, 0.0);
    tess.vertex((double) x1, (double) y, 0.0);
    tess.vertex((double) x, (double) y, 0.0);
    tess.draw();
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
  }

  public static void drawBorder(int x, int y, int x1, int y1, int color) {
    fillRect(x, y, x1, y + 1, color);
    fillRect(x, y1 - 1, x1, y1, color);
    fillRect(x, y, x + 1, y1, color);
    fillRect(x1 - 1, y, x1, y1, color);
  }

  public void drawGui(int x, int y) {
    ensureTabsPopulated();
    if (!visible) {
      return;
    }
    int guiWidth = 73;
    fillRect(x, y, x + guiWidth - 2, y + guiHeight, 0x66000000);
    drawBorder(x, y, x + guiWidth - 2, y + guiHeight, 0xFF000000);
    for (int i = 0; i < tabs.size(); ++i) {
      int transitionTop =
          mainMenu
              ? (transition + ((selectedTab == 0 && transition < 0) ? (-transition) : 0))
              : 0;
      int transitionBottom =
          mainMenu
              ? (transition
                  + ((selectedTab == tabs.size() - 1 && transition > 0) ? (-transition) : 0))
              : 0;
      if (selectedTab == i) {
        fillRect(
            x,
            i * 12 + y + transitionTop,
            x + guiWidth - 2,
            i + (y + 12 + i * 11) + transitionBottom,
            0x80555555);
      }
    }
    int yOff = y + 2;
    for (int index = 0; index < tabs.size(); ++index) {
      GuiTab tab = tabs.get(index);
      FontUtil.drawString(tab.getLabel(), (float) (x + 2), (float) yOff, 0xFFAAAAAA);
      if (selectedTab == index && !mainMenu) {
        tab.drawTabMenu(x + guiWidth, yOff - 2);
      }
      yOff += tabHeight;
    }
    if (transition > 0) {
      --transition;
    } else if (transition < 0) {
      ++transition;
    }
  }

  public int getSelectedItem() {
    return selectedItem;
  }

  public int getTabHeight() {
    return tabHeight;
  }

  public int getTransition() {
    return transition;
  }
}
