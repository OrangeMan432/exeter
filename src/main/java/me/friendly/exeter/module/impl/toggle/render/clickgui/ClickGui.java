package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.util.ArrayList;
import me.friendly.api.interfaces.Toggleable;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.ModuleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ClickGui extends Screen {
  private static ClickGui clickGui;
  //    public final CustomFont guiFont = new CustomFont("Segoe UI", 18.0f);
  private final ArrayList<Panel> panels = new ArrayList();

  public ClickGui() {
    super(Component.literal("ClickGui"));
    if (this.getPanels().isEmpty()) {
      this.load();
    }
  }

  public static ClickGui getClickGui() {
    return clickGui == null ? (clickGui = new ClickGui()) : clickGui;
  }

  private me.friendly.exeter.module.impl.toggle.render.ClickGui getClickGuiModule() {
    Module m = Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
    return m instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui
        ? (me.friendly.exeter.module.impl.toggle.render.ClickGui) m
        : null;
  }

  private void load() {
    this.panels.clear();

    int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();

    int totalPanels = ModuleType.values().length;
    int panelWidth = 90;
    int totalGuiWidth = totalPanels * panelWidth;

    int startX = (screenWidth / 2) - (totalGuiWidth / 2);

    int x = startX - 90;

    for (final ModuleType moduleType : ModuleType.values()) {
      if (moduleType == ModuleType.CLIENT) continue;
      this.panels.add(
          new Panel(moduleType.getLabel(), x += 90, 40, true) {

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
        new Panel("Client", x += 90, 40, true) {

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
                          || module.getLabel().equalsIgnoreCase("ClickGui"))) {
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
        int textWidth = FontUtil.getStringWidth(hoveredDesc);
        int centerX = this.width / 2 - textWidth / 2;
        FontUtil.drawString(hoveredDesc, centerX, 2, 0xFFCCCCCC);
      }
    }
  }

  // this will recenter the gui when the window size changes
  @Override
  public void init() {
    this.load();
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean bool) {
    int mouseX = (int) event.x();
    int mouseY = (int) event.y();
    int clickedButton = event.button();
    this.panels.forEach(panel -> panel.mouseClicked(mouseX, mouseY, clickedButton));
    return super.mouseClicked(event, bool);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    int mouseX = (int) event.x();
    int mouseY = (int) event.y();
    int releaseButton = event.button();
    this.panels.forEach(panel -> panel.mouseReleased(mouseX, mouseY, releaseButton));
    return super.mouseReleased(event);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  public final ArrayList<Panel> getPanels() {
    return this.panels;
  }
}
