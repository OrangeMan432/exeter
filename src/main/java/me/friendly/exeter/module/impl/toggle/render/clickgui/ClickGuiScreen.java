package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.ModuleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

public final class ClickGuiScreen extends Screen {
  private final List<Panel> panels = new ArrayList<Panel>();

  public ClickGuiScreen() {
    super();
    reload();
  }

  public void reload() {
    panels.clear();
    int x = 10;
    int y = 20;
    for (ModuleType type : ModuleType.values()) {
      Panel panel = new Panel(type.getLabel(), x, y, true);
      for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
        if (module instanceof ToggleableModule) {
          ToggleableModule toggleable = (ToggleableModule) module;
          if (toggleable.getModuleType() == type) {
            panel.addButton(new ModuleButton(module));
          }
        }
      }
      panels.add(panel);
      x += 100;
      if (x > 600) {
        x = 10;
        y += 60;
      }
    }
  }

  @Override
  public void render(int mouseX, int mouseY, float partialTicks) {
    super.render(mouseX, mouseY, partialTicks);
    for (Panel panel : panels) {
      panel.onMouseMove(mouseX, mouseY);
      panel.drawScreen(mouseX, mouseY, partialTicks);
    }
    String hint = "Right-click a module for settings. Right-click a header to collapse.";
    FontUtil.drawString(hint, 4.0f, (float) (this.height - 12), 0xFF888888);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    for (int i = panels.size() - 1; i >= 0; i--) {
      Panel panel = panels.get(i);
      if (panel.containsMouse(mouseX, mouseY)) {
        panel.mouseClicked(mouseX, mouseY, mouseButton);
        return;
      }
    }
    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  protected void mouseReleased(int mouseX, int mouseY, int releaseButton) {
    for (Panel panel : panels) {
      panel.mouseReleased(mouseX, mouseY, releaseButton);
    }
    super.mouseReleased(mouseX, mouseY, releaseButton);
  }

  @Override
  protected void keyPressed(char typedChar, int keyCode) {
    if (keyCode == 1) {
      Minecraft mc = this.minecraft;
      if (mc != null) {
        mc.setScreen(null);
      }
      return;
    }
    super.keyPressed(typedChar, keyCode);
  }

  @Override
  public boolean shouldPause() {
    return false;
  }

  public List<Panel> getPanels() {
    return this.panels;
  }
}
