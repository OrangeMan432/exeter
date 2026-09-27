package me.friendly.exeter.module.impl.toggle.render.clickgui;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import me.friendly.api.interfaces.Toggleable;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.ModuleButton;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties.BooleanButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ClickGui extends Screen {
  private static ClickGui clickGui;
  private final ArrayList<Panel> panels = new ArrayList();
  private SearchSelectPopup popup;
  private String search = "";

  // Expansion state kept in memory so reopening the GUI restores it. Never persisted to disk.
  private final Map<String, Boolean> subOpenMemory = new HashMap<>();
  private final Map<String, Boolean> childOpenMemory = new HashMap<>();

  public ClickGui() {
    super(Component.literal("ClickGui"));
    if (this.getPanels().isEmpty()) {
      this.load();
    }
  }

  public static ClickGui getClickGui() {
    return clickGui == null ? (clickGui = new ClickGui()) : clickGui;
  }

  public void openPopup(SearchSelectPopup popup) {
    this.popup = popup;
  }

  public void closePopup() {
    this.popup = null;
  }

  public SearchSelectPopup getPopup() {
    return this.popup;
  }

  private me.friendly.exeter.module.impl.toggle.render.ClickGui getClickGuiModule() {
    Module m = Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
    return m instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui
        ? (me.friendly.exeter.module.impl.toggle.render.ClickGui) m
        : null;
  }

  private void load() {
    this.panels.clear();
    me.friendly.exeter.module.impl.toggle.render.ClickGui guiModule = getClickGuiModule();
    me.friendly.exeter.module.impl.toggle.render.ClickGui.PanelAlignment alignment =
        guiModule != null
            ? guiModule.panelAlignment.getValue()
            : me.friendly.exeter.module.impl.toggle.render.ClickGui.PanelAlignment.CENTERED;

    int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
    int totalPanels = ModuleType.values().length - 1; // exclude HUD, CLIENT added manually
    int panelWidth = 90;
    int totalGuiWidth = totalPanels * panelWidth;

    int x;
    int y;
    if (alignment
        == me.friendly.exeter.module.impl.toggle.render.ClickGui.PanelAlignment.TOP_LEFT) {
      x = 4 - 90;
      y = 4;
    } else {
      x = (screenWidth / 2) - (totalGuiWidth / 2) - 90;
      y = 40;
    }

    for (final ModuleType moduleType : ModuleType.values()) {
      if (moduleType == ModuleType.CLIENT || moduleType == ModuleType.HUD) continue;
      this.panels.add(
          new Panel(moduleType.getLabel(), x += 90, y, true) {
            @Override
            public void setupItems() {
              Exeter.getInstance()
                  .getModuleManager()
                  .getRegistry()
                  .forEach(
                      module -> {
                        ToggleableModule toggleableModule;
                        if (module instanceof Toggleable
                            && !module.getLabel().equalsIgnoreCase("ClickGui")
                            && (toggleableModule = (ToggleableModule) module)
                                .getModuleType()
                                .equals((Object) moduleType)) {
                          this.addButton(new ModuleButton(toggleableModule));
                        }
                      });
            }
          });
    }
    this.panels.add(
        new Panel("Client", x += 90, y, true) {
          @Override
          public void setupItems() {
            Exeter.getInstance()
                .getModuleManager()
                .getRegistry()
                .forEach(
                    module -> {
                      if (module instanceof ToggleableModule toggleable
                          && toggleable.getModuleType() == ModuleType.CLIENT) {
                        this.addButton(new ModuleButton(toggleable));
                      } else if (module
                          instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui) {
                        this.addButton(new ModuleButton(module));
                      } else if (!(module instanceof Toggleable
                          || module.getLabel().equalsIgnoreCase("ClickGui")
                          || module.getLabel().equalsIgnoreCase("HudRenderer"))) {
                        this.addButton(new ModuleButton((Module) module));
                      }
                    });
          }
        });
    this.panels.forEach(
        panel ->
            panel.getItems().sort((item1, item2) -> item1.getLabel().compareTo(item2.getLabel())));
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
    RenderMethods.guiGraphics = guiGraphics;

    me.friendly.exeter.module.impl.toggle.render.ClickGui guiModule = getClickGuiModule();
    boolean showBg = guiModule == null || guiModule.showBackground.getValue();
    boolean showDesc = guiModule == null || guiModule.showDescriptions.getValue();
    me.friendly.exeter.module.impl.toggle.render.ClickGui.DescriptionMode descMode =
        guiModule != null ? guiModule.getDescriptionMode() : null;

    if (showBg) {
      RenderMethods.drawGradientRect(0.0F, 0.0F, this.width, this.height, 536870912, -1879048192);
    }
    this.panels.forEach(panel -> panel.drawScreen(mouseX, mouseY, partialTicks));

    me.friendly.exeter.module.impl.toggle.render.ClickGui guiMod2 = getClickGuiModule();
    boolean searchEnabled = guiMod2 == null || guiMod2.searchEnabled.getValue();
    if (searchEnabled && !search.isEmpty()) {
      int queryWidth = FontUtil.getStringWidth(search);
      int sx = this.width / 2 - queryWidth / 2;
      FontUtil.drawString(search, sx, 15, 0xFFFFFFFF);
    }

    if (showDesc) {
      String hoveredDesc = null;
      for (Panel panel : this.panels) {
        if (!panel.getOpen()) continue;
        for (var item : panel.getItems()) {
          if (item instanceof ModuleButton moduleButton && panel.matchesSearch(item)) {
            float ix = item.getX();
            float iy = item.getY();
            int iw = item.getWidth();
            int ih = item.getHeight();
            if (mouseX >= ix && mouseX <= ix + iw && mouseY >= iy && mouseY <= iy + ih) {
              String desc = moduleButton.getModule().getDescription();
              if (desc != null && !desc.isEmpty()) {
                hoveredDesc = desc;
              }
            }
          }
        }
      }

      if (hoveredDesc != null) {
        if (descMode
            == me.friendly.exeter.module.impl.toggle.render.ClickGui.DescriptionMode.PANEL) {
          Panel lastPanel = this.panels.isEmpty() ? null : this.panels.get(this.panels.size() - 1);
          int descX = lastPanel != null ? lastPanel.getX() + lastPanel.getWidth() + 2 : 4;
          int descY = lastPanel != null ? lastPanel.getY() : 4;
          int headerW = FontUtil.getStringWidth("Description");
          int textW = FontUtil.getStringWidth(hoveredDesc);
          int padding = 4;
          int descW = Math.max(headerW, textW) + padding * 2;
          int headerH = 18;
          int descH = headerH - 6 + padding + 8 + padding;

          RenderMethods.drawGradientRect(
              descX,
              descY - 1.5f,
              descX + descW,
              descY + headerH - 6,
              Colors.getClientColorCustomAlpha(77),
              Colors.getClientColorCustomAlpha(77));
          RenderMethods.drawRect(
              descX, descY + headerH - 6, descX + descW, descY + descH, 0x77000000);
          FontUtil.drawString("Description", descX + 3.0f, descY + 1.5f, -1);
          FontUtil.drawString(
              hoveredDesc, descX + padding, descY + headerH - 6 + padding, 0xFFCCCCCC);
        } else {
          int textWidth = FontUtil.getStringWidth(hoveredDesc);
          int centerX = this.width / 2 - textWidth / 2;
          FontUtil.drawString(hoveredDesc, centerX, 2, 0xFFCCCCCC);
        }
      }
    }

    if (popup != null) {
      popup.render(mouseX, mouseY, partialTicks, this.width, this.height);
    }
  }

  @Override
  public void init() {
    snapshotOpenStates();
    this.load();
    restoreOpenStates();
    applySavedPositions();
  }

  private void snapshotOpenStates() {
    subOpenMemory.clear();
    childOpenMemory.clear();
    for (Panel panel : panels) {
      for (var item : panel.getItems()) {
        if (item instanceof ModuleButton moduleButton) {
          subOpenMemory.put(moduleButton.getModule().getLabel(), moduleButton.isSubOpen());
          for (var child : moduleButton.getTopLevelItems()) {
            if (child instanceof BooleanButton booleanChild) {
              childOpenMemory.put(
                  moduleButton.getModule().getLabel() + ">" + booleanChild.getLabel(),
                  booleanChild.isChildrenOpen());
            }
          }
        }
      }
    }
  }

  private void restoreOpenStates() {
    for (Panel panel : panels) {
      for (var item : panel.getItems()) {
        if (item instanceof ModuleButton moduleButton) {
          Boolean open = subOpenMemory.get(moduleButton.getModule().getLabel());
          if (open != null) {
            moduleButton.setSubOpen(open);
          }
          for (var child : moduleButton.getTopLevelItems()) {
            if (child instanceof BooleanButton booleanChild) {
              Boolean childOpen =
                  childOpenMemory.get(
                      moduleButton.getModule().getLabel() + ">" + booleanChild.getLabel());
              if (childOpen != null) {
                booleanChild.setChildrenOpen(childOpen);
              }
            }
          }
        }
      }
    }
  }

  /** Applies positions from clickgui.toml on top of the default layout. */
  private void applySavedPositions() {
    me.friendly.exeter.module.impl.toggle.render.ClickGui guiModule = getClickGuiModule();
    if (guiModule == null) {
      return;
    }
    for (Panel panel : panels) {
      int[] pos = guiModule.getPendingPanels().get(panel.getLabel());
      if (pos != null) {
        panel.setX(pos[0]);
        panel.setY(pos[1]);
      }
    }
  }

  /** Copies live panel positions into the module for saving. */
  private void capturePositions() {
    me.friendly.exeter.module.impl.toggle.render.ClickGui guiModule = getClickGuiModule();
    if (guiModule == null) {
      return;
    }
    guiModule.getPendingPanels().clear();
    for (Panel panel : panels) {
      guiModule.getPendingPanels().put(panel.getLabel(), new int[] {panel.getX(), panel.getY()});
    }
  }

  /** Restores the default layout and persists it. */
  public void resetPanels() {
    me.friendly.exeter.module.impl.toggle.render.ClickGui guiModule = getClickGuiModule();
    if (guiModule != null) {
      guiModule.getPendingPanels().clear();
    }
    this.load();
    capturePositions();
    if (guiModule != null) {
      me.friendly.exeter.config.ExeterConfig.getInstance().saveModule(guiModule);
    }
  }

  @Override
  public void removed() {
    super.removed();
    capturePositions();
    me.friendly.exeter.module.impl.toggle.render.ClickGui guiModule = getClickGuiModule();
    if (guiModule != null) {
      me.friendly.exeter.config.ExeterConfig.getInstance().saveModule(guiModule);
    }
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean bool) {
    int mouseX = (int) event.x();
    int mouseY = (int) event.y();
    int clickedButton = event.button();

    if (popup != null) {
      if (popup.mouseClicked(mouseX, mouseY, clickedButton)) {
        return true;
      }
    }

    // Copy: button actions (e.g. Reset Positions) may rebuild this list mid-click.
    new ArrayList<>(this.panels)
        .forEach(panel -> panel.mouseClicked(mouseX, mouseY, clickedButton));
    return super.mouseClicked(event, bool);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    int mouseX = (int) event.x();
    int mouseY = (int) event.y();
    int releaseButton = event.button();

    if (popup != null) {
      if (popup.mouseReleased(mouseX, mouseY, releaseButton)) {
        return true;
      }
    }

    this.panels.forEach(panel -> panel.mouseReleased(mouseX, mouseY, releaseButton));
    return super.mouseReleased(event);
  }

  @Override
  public boolean mouseDragged(
      net.minecraft.client.input.MouseButtonEvent event, double deltaX, double deltaY) {
    if (popup != null) {
      int mouseX = (int) event.x();
      int mouseY = (int) event.y();
      if (popup.mouseDragged(mouseX, mouseY)) {
        return true;
      }
    }
    return super.mouseDragged(event, deltaX, deltaY);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollDelta) {
    if (popup != null) {
      if (popup.mouseScrolled(scrollDelta)) {
        return true;
      }
    }

    int scroll = (int) (scrollDelta * 12);
    boolean hoveringPanel = false;
    for (Panel panel : this.panels) {
      if (panel.containsMouse((int) mouseX, (int) mouseY)) {
        hoveringPanel = true;
        break;
      }
    }
    if (hoveringPanel) {
      for (Panel panel : this.panels) {
        panel.scroll(scroll);
      }
      return true;
    }

    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollDelta);
  }

  @Override
  public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
    if (popup != null) {
      if (popup.keyPressed(event.key(), event.keycode(), event.modifiers())) {
        return true;
      }
    }
    me.friendly.exeter.module.impl.toggle.render.ClickGui guiModKey = getClickGuiModule();
    boolean searchOn = guiModKey == null || guiModKey.searchEnabled.getValue();
    int key = event.key();
    if (searchOn && key == InputConstants.KEY_BACKSPACE && !search.isEmpty()) {
      search = search.substring(0, search.length() - 1);
      return true;
    }
    if (searchOn && key == InputConstants.KEY_ESCAPE && !search.isEmpty()) {
      search = "";
      return true;
    }
    if (searchOn && popup == null && search.length() < 24) {
      if (key >= InputConstants.KEY_A && key <= InputConstants.KEY_Z) {
        search += (char) ('a' + key - InputConstants.KEY_A);
        return true;
      }
      if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_9) {
        search += (char) ('1' + key - InputConstants.KEY_1);
        return true;
      }
      if (key == InputConstants.KEY_0) {
        search += '0';
        return true;
      }
      if (key == InputConstants.KEY_SPACE) {
        search += " ";
        return true;
      }
    }
    return super.keyPressed(event);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  public final ArrayList<Panel> getPanels() {
    return this.panels;
  }

  public String getSearch() {
    return this.search;
  }
}
