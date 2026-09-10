package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.util.ArrayList;
import me.friendly.api.interfaces.Toggleable;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.ModuleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ClickGui extends Screen {
  private static ClickGui clickGui;
  private final ArrayList<Panel> panels = new ArrayList();
  private SearchSelectPopup popup;

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
        guiModule != null ? guiModule.panelAlignment.getValue()
            : me.friendly.exeter.module.impl.toggle.render.ClickGui.PanelAlignment.CENTERED;

    int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
    int totalPanels = ModuleType.values().length;
    int panelWidth = 90;
    int totalGuiWidth = totalPanels * panelWidth;

    int x;
    int y;
    if (alignment == me.friendly.exeter.module.impl.toggle.render.ClickGui.PanelAlignment.TOP_LEFT) {
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

    if (showDesc) {
      String hoveredDesc = null;
      for (Panel panel : this.panels) {
        if (!panel.getOpen()) continue;
        for (var item : panel.getItems()) {
          if (item instanceof ModuleButton moduleButton) {
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
        if (descMode == me.friendly.exeter.module.impl.toggle.render.ClickGui.DescriptionMode.PANEL) {
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
              descX, descY - 1.5f, descX + descW, descY + headerH - 6,
              Colors.getClientColorCustomAlpha(77), Colors.getClientColorCustomAlpha(77));
          RenderMethods.drawRect(
              descX, descY + headerH - 6, descX + descW, descY + descH, 0x77000000);
          FontUtil.drawString("Description", descX + 3.0f, descY + 1.5f, -1);
          FontUtil.drawString(hoveredDesc, descX + padding, descY + headerH - 6 + padding, 0xFFCCCCCC);
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
    this.load();
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

    this.panels.forEach(panel -> panel.mouseClicked(mouseX, mouseY, clickedButton));
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
      if (popup.keyPressed(event.key(), event.scancode(), event.modifiers())) {
        return true;
      }
    }
    return super.keyPressed(event);
  }

  @Override
  public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
    if (popup != null) {
      char c = (char) event.codepoint();
      if (popup.charTyped(c, 0)) {
        return true;
      }
    }
    return super.charTyped(event);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  public final ArrayList<Panel> getPanels() {
    return this.panels;
  }
}
