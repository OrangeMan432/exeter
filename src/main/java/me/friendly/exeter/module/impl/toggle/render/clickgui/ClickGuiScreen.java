package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Item;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Item;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.ModuleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

public final class ClickGuiScreen extends Screen {
  private static ClickGuiScreen instance;
  private final List<Panel> panels = new ArrayList<Panel>();
  private SearchSelectPopup popup;
  private String search = "";

  public ClickGuiScreen() {
    super();
  }

  @Override
  public void init() {
    super.init();
    reload();
  }

  public static ClickGuiScreen getInstance() {
    if (instance == null) {
      instance = new ClickGuiScreen();
    }
    return instance;
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

  public void reload() {
    panels.clear();
    int panelWidth = 90;
    int totalPanels = 0;
    for (ModuleType type : ModuleType.values()) {
      if (type != ModuleType.HUD) totalPanels++;
    }
    int totalGuiWidth = totalPanels * panelWidth;
    int x = (this.width / 2) - (totalGuiWidth / 2) - panelWidth;
    int y = 40;
    for (ModuleType type : ModuleType.values()) {
      if (type == ModuleType.HUD || type == ModuleType.CLIENT) continue;
      Panel panel = new Panel(type.getLabel(), x += panelWidth, y, true);
      addModuleButtons(panel, type);
      sortPanelItems(panel);
      panels.add(panel);
    }
    Panel clientPanel = new Panel(ModuleType.CLIENT.getLabel(), x += panelWidth, y, true);
    addModuleButtons(clientPanel, ModuleType.CLIENT);
    sortPanelItems(clientPanel);
    panels.add(clientPanel);
  }

  private void addModuleButtons(Panel panel, ModuleType type) {
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      if (module instanceof ToggleableModule) {
        ToggleableModule toggleable = (ToggleableModule) module;
        if (toggleable.getModuleType() == type) {
          panel.addButton(new ModuleButton(module));
        }
      } else if (type == ModuleType.CLIENT) {
        panel.addButton(new ModuleButton(module));
      }
    }
  }

  private void sortPanelItems(Panel panel) {
    List<Item> items = panel.getItems();
    for (int i = 0; i < items.size(); i++) {
      for (int j = i + 1; j < items.size(); j++) {
        if (items.get(j).getLabel().compareTo(items.get(i).getLabel()) < 0) {
          Item tmp = items.get(i);
          items.set(i, items.get(j));
          items.set(j, tmp);
        }
      }
    }
  }

  private me.friendly.exeter.module.impl.toggle.render.ClickGui guiModule() {
    if (Exeter.getInstance() == null) return null;
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
    if (module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui) {
      return (me.friendly.exeter.module.impl.toggle.render.ClickGui) module;
    }
    return null;
  }

  @Override
  public void render(int mouseX, int mouseY, float partialTicks) {
    super.render(mouseX, mouseY, partialTicks);
    me.friendly.exeter.module.impl.toggle.render.ClickGui guiMod = guiModule();
    if (guiMod == null || guiMod.showBackground.getValue().booleanValue()) {
      fillGradient(0, 0, this.width, this.height, 0x80000000, 0x40000000);
    }
    for (Panel panel : panels) {
      panel.drawScreen(mouseX, mouseY, partialTicks);
    }

    boolean searchOn = guiMod == null || guiMod.searchEnabled.getValue().booleanValue();
    if (searchOn && !search.isEmpty()) {
      int queryWidth = FontUtil.getStringWidth(search);
      FontUtil.drawString(search, (float) (this.width / 2 - queryWidth / 2), 15.0f, 0xFFFFFFFF);
    }

    boolean showDesc = guiMod == null || guiMod.showDescriptions.getValue().booleanValue();
    if (showDesc) {
      String hoveredDesc = findHoveredDescription(mouseX, mouseY);
      if (hoveredDesc != null && !hoveredDesc.isEmpty()) {
        int textWidth = FontUtil.getStringWidth(hoveredDesc);
        FontUtil.drawString(hoveredDesc, (float) (this.width / 2 - textWidth / 2), 2.0f, 0xFFCCCCCC);
      }
    }

    if (popup != null) {
      popup.render(mouseX, mouseY, partialTicks, this.width, this.height);
    }
  }

  private String findHoveredDescription(int mouseX, int mouseY) {
    for (Panel panel : panels) {
      if (!panel.getOpen()) continue;
      for (Item item : panel.getItems()) {
        if (item instanceof ModuleButton) {
          ModuleButton button = (ModuleButton) item;
          float ix = button.getX();
          float iy = button.getY();
          if (mouseX >= ix && mouseX <= ix + button.getWidth() && mouseY >= iy && mouseY <= iy + 12) {
            return button.getModule().getDescription();
          }
        }
      }
    }
    return null;
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (popup != null) {
      if (popup.mouseClicked(mouseX, mouseY, mouseButton)) {
        return;
      }
    }
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
    if (popup != null) {
      popup.keyPressed(typedChar, keyCode);
      return;
    }    me.friendly.exeter.module.impl.toggle.render.ClickGui guiMod = guiModule();
    boolean searchOn = guiMod == null || guiMod.searchEnabled.getValue().booleanValue();
    if (searchOn) {
      if (keyCode == 14 && search.length() > 0) {
        search = search.substring(0, search.length() - 1);
        return;
      }
      if (keyCode == 1 && search.length() > 0) {
        search = "";
        return;
      }
      if (search.length() < 24) {
        char lower = Character.toLowerCase(typedChar);
        if ((lower >= 'a' && lower <= 'z')
            || (lower >= '0' && lower <= '9')
            || lower == ' ') {
          search += lower;
          return;
        }
      }
    }
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

  public String getSearch() {
    return this.search;
  }
}
