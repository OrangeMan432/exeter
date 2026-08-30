package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.InputEvent;
import me.friendly.exeter.events.RenderGameOverlayEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.active.render.Hud;
import me.friendly.exeter.module.impl.toggle.render.tabgui.GuiTabHandler;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.Property;
import org.lwjgl.glfw.GLFW;

public final class TabGui extends ToggleableModule {
  private GuiTabHandler guiTabHandler;
  private final EnumProperty<Mode> mode = new EnumProperty<Mode>(Mode.DEFAULT, "mode", "m");
  public int hexVal = -1152209207;

  public TabGui() {
    super("TabGui", new String[] {"tabgui", "tg"}, ModuleType.RENDER);
    this.guiTabHandler = new GuiTabHandler();
    this.listeners.add(
        new Listener<RenderGameOverlayEvent>("tab_gui_render_game_overlay_listener") {

          @Override
          public void call(RenderGameOverlayEvent event) {
            Hud hud = (Hud) Exeter.getInstance().getModuleManager().getModuleByAlias("textgui");
            Property watermark = hud.getPropertyByAlias("Watermark");
            if (((TabGui) TabGui.this).minecraft.gui.hud.getDebugOverlay().showDebugScreen()) {
              return;
            }
            TabGui.this.guiTabHandler.drawGui(3, (Boolean) watermark.getValue() != false ? 13 : 3);
          }
        });
    this.listeners.add(
        new Listener<InputEvent>("tab_gui_input_listener") {

          @Override
          public void call(InputEvent event) {
            if (event.getType() == InputEvent.Type.KEYBOARD_KEY_PRESS) {
              TabGui.this.guiTabHandler.ensureTabsPopulated();
              switch (event.getKey()) {
                case GLFW.GLFW_KEY_UP:
                  {
                    if (!((TabGui) TabGui.this).guiTabHandler.visible) break;
                    if (((TabGui) TabGui.this).guiTabHandler.mainMenu) {
                      --((TabGui) TabGui.this).guiTabHandler.selectedTab;
                      if (((TabGui) TabGui.this).guiTabHandler.selectedTab < 0) {
                        ((TabGui) TabGui.this).guiTabHandler.selectedTab =
                            ((TabGui) TabGui.this).guiTabHandler.tabs.size() - 1;
                      }
                      ((TabGui) TabGui.this).guiTabHandler.transition = 11;
                      break;
                    }
                    --((TabGui) TabGui.this).guiTabHandler.selectedItem;
                    if (((TabGui) TabGui.this).guiTabHandler.selectedItem < 0) {
                      ((TabGui) TabGui.this).guiTabHandler.selectedItem =
                          ((TabGui) TabGui.this)
                                  .guiTabHandler
                                  .tabs
                                  .get(((TabGui) TabGui.this).guiTabHandler.selectedTab)
                                  .getMods()
                                  .size()
                              - 1;
                    }
                    if (((TabGui) TabGui.this)
                            .guiTabHandler
                            .tabs
                            .get(((TabGui) TabGui.this).guiTabHandler.selectedTab)
                            .getMods()
                            .size()
                        <= 1) break;
                    ((TabGui) TabGui.this).guiTabHandler.transition = 11;
                    break;
                  }
                case GLFW.GLFW_KEY_DOWN:
                  {
                    if (!((TabGui) TabGui.this).guiTabHandler.visible) break;
                    if (((TabGui) TabGui.this).guiTabHandler.mainMenu) {
                      ++((TabGui) TabGui.this).guiTabHandler.selectedTab;
                      if (((TabGui) TabGui.this).guiTabHandler.selectedTab
                          > ((TabGui) TabGui.this).guiTabHandler.tabs.size() - 1) {
                        ((TabGui) TabGui.this).guiTabHandler.selectedTab = 0;
                      }
                      ((TabGui) TabGui.this).guiTabHandler.transition = -11;
                      break;
                    }
                    ++((TabGui) TabGui.this).guiTabHandler.selectedItem;
                    if (((TabGui) TabGui.this).guiTabHandler.selectedItem
                        > ((TabGui) TabGui.this)
                                .guiTabHandler
                                .tabs
                                .get(((TabGui) TabGui.this).guiTabHandler.selectedTab)
                                .getMods()
                                .size()
                            - 1) {
                      ((TabGui) TabGui.this).guiTabHandler.selectedItem = 0;
                    }
                    if (((TabGui) TabGui.this)
                            .guiTabHandler
                            .tabs
                            .get(((TabGui) TabGui.this).guiTabHandler.selectedTab)
                            .getMods()
                            .size()
                        <= 1) break;
                    ((TabGui) TabGui.this).guiTabHandler.transition = -11;
                    break;
                  }
                case GLFW.GLFW_KEY_LEFT:
                  {
                    if (((TabGui) TabGui.this).guiTabHandler.mainMenu) break;
                    ((TabGui) TabGui.this).guiTabHandler.mainMenu = true;
                    break;
                  }
                case GLFW.GLFW_KEY_RIGHT:
                  {
                    if (((TabGui) TabGui.this).guiTabHandler.mainMenu) {
                      ((TabGui) TabGui.this).guiTabHandler.mainMenu = false;
                      ((TabGui) TabGui.this).guiTabHandler.selectedItem = 0;
                      break;
                    }
                    if (!((TabGui) TabGui.this).guiTabHandler.visible) {
                      ((TabGui) TabGui.this).guiTabHandler.visible = true;
                      ((TabGui) TabGui.this).guiTabHandler.mainMenu = true;
                      break;
                    }
                    ((TabGui) TabGui.this)
                        .guiTabHandler
                        .tabs
                        .get(((TabGui) TabGui.this).guiTabHandler.selectedTab)
                        .getMods()
                        .get(((TabGui) TabGui.this).guiTabHandler.selectedItem)
                        .getToggleableModule()
                        .toggle();
                    break;
                  }
                case GLFW.GLFW_KEY_ENTER:
                  {
                    if (((TabGui) TabGui.this).guiTabHandler.mainMenu
                        || !((TabGui) TabGui.this).guiTabHandler.visible) break;
                    ((TabGui) TabGui.this)
                        .guiTabHandler
                        .tabs
                        .get(((TabGui) TabGui.this).guiTabHandler.selectedTab)
                        .getMods()
                        .get(((TabGui) TabGui.this).guiTabHandler.selectedItem)
                        .getToggleableModule()
                        .toggle();
                  }
              }
            }
          }
        });
    this.setRunning(true);
  }

  private static enum Mode {
    DEFAULT,
    BLUE,
    PURPLE;
  }
}
