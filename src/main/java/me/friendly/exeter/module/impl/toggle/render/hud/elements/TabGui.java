package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import com.mojang.blaze3d.platform.InputConstants;
import me.friendly.api.event.Listener;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.events.InputEvent;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.module.impl.toggle.render.tabgui.GuiTabHandler;
import me.friendly.exeter.module.impl.toggle.render.tabgui.item.GuiTab;
import me.friendly.exeter.properties.EnumProperty;

public final class TabGui extends HudModule {
  private final GuiTabHandler guiTabHandler = new GuiTabHandler();
  private final EnumProperty<Mode> mode = new EnumProperty<Mode>(Mode.DEFAULT, "mode", "m");
  public int hexVal = -1152209207;

  public TabGui() {
    super("TabGui", new String[] {"tabgui", "tg"}, Corner.TOP_LEFT);
    setDescription("Renders a category-based tab menu for quick module access.");
    this.listeners.add(
        new Listener<InputEvent>("tab_gui_input_listener") {

          @Override
          public void call(InputEvent event) {
            if (event.getType() == InputEvent.Type.KEYBOARD_KEY_PRESS) {
              guiTabHandler.ensureTabsPopulated();
              switch (event.getKey()) {
                case InputConstants.KEY_UP:
                  {
                    if (!guiTabHandler.visible) break;
                    if (guiTabHandler.mainMenu) {
                      --guiTabHandler.selectedTab;
                      if (guiTabHandler.selectedTab < 0) {
                        guiTabHandler.selectedTab = guiTabHandler.tabs.size() - 1;
                      }
                      guiTabHandler.transition = 11;
                      break;
                    }
                    --guiTabHandler.selectedItem;
                    if (guiTabHandler.selectedItem < 0) {
                      guiTabHandler.selectedItem =
                          guiTabHandler.tabs.get(guiTabHandler.selectedTab).getMods().size() - 1;
                    }
                    if (guiTabHandler.tabs.get(guiTabHandler.selectedTab).getMods().size() <= 1)
                      break;
                    guiTabHandler.transition = 11;
                    break;
                  }
                case InputConstants.KEY_DOWN:
                  {
                    if (!guiTabHandler.visible) break;
                    if (guiTabHandler.mainMenu) {
                      ++guiTabHandler.selectedTab;
                      if (guiTabHandler.selectedTab > guiTabHandler.tabs.size() - 1) {
                        guiTabHandler.selectedTab = 0;
                      }
                      guiTabHandler.transition = -11;
                      break;
                    }
                    ++guiTabHandler.selectedItem;
                    if (guiTabHandler.selectedItem
                        > guiTabHandler.tabs.get(guiTabHandler.selectedTab).getMods().size() - 1) {
                      guiTabHandler.selectedItem = 0;
                    }
                    if (guiTabHandler.tabs.get(guiTabHandler.selectedTab).getMods().size() <= 1)
                      break;
                    guiTabHandler.transition = -11;
                    break;
                  }
                case InputConstants.KEY_LEFT:
                  {
                    if (guiTabHandler.mainMenu) break;
                    guiTabHandler.mainMenu = true;
                    break;
                  }
                case InputConstants.KEY_RIGHT:
                  {
                    if (guiTabHandler.mainMenu) {
                      guiTabHandler.mainMenu = false;
                      guiTabHandler.selectedItem = 0;
                      break;
                    }
                    if (!guiTabHandler.visible) {
                      guiTabHandler.visible = true;
                      guiTabHandler.mainMenu = true;
                      break;
                    }
                    guiTabHandler.tabs
                        .get(guiTabHandler.selectedTab)
                        .getMods()
                        .get(guiTabHandler.selectedItem)
                        .getToggleableModule()
                        .toggle();
                    break;
                  }
                case InputConstants.KEY_RETURN:
                  {
                    if (guiTabHandler.mainMenu || !guiTabHandler.visible) break;
                    guiTabHandler.tabs
                        .get(guiTabHandler.selectedTab)
                        .getMods()
                        .get(guiTabHandler.selectedItem)
                        .getToggleableModule()
                        .toggle();
                    break;
                  }
                default:
                  break;
              }
            }
          }
        });
    this.setRunning(true);
  }

  @Override
  public int getWidth() {
    guiTabHandler.ensureTabsPopulated();
    int width = 71;
    if (guiTabHandler.visible
        && !guiTabHandler.mainMenu
        && guiTabHandler.selectedTab >= 0
        && guiTabHandler.selectedTab < guiTabHandler.tabs.size()) {
      GuiTab tab = guiTabHandler.tabs.get(guiTabHandler.selectedTab);
      int max = 0;
      for (var item : tab.getMods()) {
        int w = FontUtil.getStringWidth(item.getToggleableModule().getAliases()[0]);
        if (w > max) max = w;
      }
      width = 73 + max + 4 + 5;
    }
    return width;
  }

  @Override
  public int getHeight() {
    guiTabHandler.ensureTabsPopulated();
    int height = guiTabHandler.tabs.size() * guiTabHandler.getTabHeight();
    if (guiTabHandler.visible
        && !guiTabHandler.mainMenu
        && guiTabHandler.selectedTab >= 0
        && guiTabHandler.selectedTab < guiTabHandler.tabs.size()) {
      GuiTab tab = guiTabHandler.tabs.get(guiTabHandler.selectedTab);
      int submenuBottom =
          guiTabHandler.selectedTab * guiTabHandler.getTabHeight()
              + tab.getMods().size() * guiTabHandler.getTabHeight();
      if (submenuBottom > height) height = submenuBottom;
    }
    return height;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    guiTabHandler.drawGui(getX(), getY());
  }

  private static enum Mode {
    DEFAULT,
    BLUE,
    PURPLE;
  }
}
