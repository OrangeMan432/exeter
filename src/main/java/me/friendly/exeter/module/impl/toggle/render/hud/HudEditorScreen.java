package me.friendly.exeter.module.impl.toggle.render.hud;

import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.Panel;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Item;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.ModuleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

public final class HudEditorScreen extends Screen {
  private static HudEditorScreen instance;

  private HudModule dragging;
  private int dragOffX;
  private int dragOffY;

  private Panel hudPanel;
  private int lastRegistrySize = -1;

  public HudEditorScreen() {
    super();
  }

  public static HudEditorScreen getInstance() {
    if (instance == null) {
      instance = new HudEditorScreen();
    }
    return instance;
  }

  private void ensurePanel() {
    if (hudPanel != null) return;
    hudPanel = new Panel("HUD", 50, 50, true);
  }

  private void rebuildItems() {
    List<Module> registry = Exeter.getInstance().getModuleManager().getRegistry();
    if (registry.size() == lastRegistrySize && hudPanel.getItems().size() > 0) {
      return;
    }
    lastRegistrySize = registry.size();
    hudPanel.getItems().clear();
    for (int i = 0; i < registry.size(); i++) {
      Module module = registry.get(i);
      if (module instanceof HudModule) {
        hudPanel.addButton(new ModuleButton(module));
      }
    }
  }

  @Override
  public void render(int mouseX, int mouseY, float partialTicks) {
    super.render(mouseX, mouseY, partialTicks);
    if (isBackgroundEnabled()) {
      fillGradient(0, 0, this.width, this.height, 0x80000000, 0x40000000);
    }
    ensurePanel();
    rebuildItems();

    if (dragging != null) {
      dragging.setX(mouseX - dragOffX);
      dragging.setY(mouseY - dragOffY);
    }

    List<HudModule> modules = HudModule.getActive();
    HudModule.layoutByCorner(modules, this.width, this.height);
    for (HudModule m : modules) {
      m.render(this.width, this.height);
      int mx = m.getX();
      int my = m.getY();
      int mw = Math.max(m.getWidth(), 20);
      int mh = Math.max(m.getHeight(), 10);
      boolean hovered =
          mouseX >= mx && mouseX <= mx + mw && mouseY >= my && mouseY <= my + mh;
      int outline = hovered ? 0xFFFFFFFF : 0x80FFFFFF;
      drawHOutline(mx - 1, mx + mw, my - 1, outline);
      drawHOutline(mx - 1, mx + mw, my + mh, outline);
      drawVOutline(mx - 1, my - 1, my + mh, outline);
      drawVOutline(mx + mw, my - 1, my + mh, outline);
    }

    drawSnapGuides(this.width, this.height);

    hudPanel.drawScreen(mouseX, mouseY, partialTicks);

    me.friendly.exeter.module.impl.toggle.render.ClickGui guiMod = clickGuiModule();
    boolean showDesc = guiMod == null || guiMod.showDescriptions.getValue().booleanValue();
    if (showDesc) {
      String hoveredDesc = findHoveredDescription(mouseX, mouseY);
      if (hoveredDesc != null && !hoveredDesc.isEmpty()) {
        int textWidth = FontUtil.getStringWidth(hoveredDesc);
        FontUtil.drawString(
            hoveredDesc, (float) (this.width / 2 - textWidth / 2), 2.0f, 0xFFCCCCCC);
      }
    }
  }

  private boolean isBackgroundEnabled() {
    me.friendly.exeter.module.impl.toggle.render.ClickGui guiMod = clickGuiModule();
    return guiMod == null || guiMod.showBackground.getValue().booleanValue();
  }

  private me.friendly.exeter.module.impl.toggle.render.ClickGui clickGuiModule() {    if (Exeter.getInstance() == null) return null;
    Module module =
        Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
    if (module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui) {
      return (me.friendly.exeter.module.impl.toggle.render.ClickGui) module;
    }
    return null;
  }

  private String findHoveredDescription(int mouseX, int mouseY) {
    if (hudPanel == null || !hudPanel.getOpen()) return null;
    for (Item item : hudPanel.getItems()) {
      if (item instanceof ModuleButton) {
        ModuleButton button = (ModuleButton) item;
        float ix = button.getX();
        float iy = button.getY();
        if (mouseX >= ix
            && mouseX <= ix + button.getWidth()
            && mouseY >= iy
            && mouseY <= iy + 12) {
          return button.getModule().getDescription();
        }
      }
    }
    return null;
  }

  private void drawSnapGuides(int scaledWidth, int scaledHeight) {
    int snapMargin = 5;
    int half = scaledWidth / 2;
    fill(snapMargin, 0, snapMargin + 1, scaledHeight, 0x40FFFF00);
    fill(scaledWidth - snapMargin, 0, scaledWidth - snapMargin + 1, scaledHeight, 0x40FFFF00);
    fill(half, 0, half + 1, scaledHeight, 0x40FFFF00);
    fill(0, snapMargin, scaledWidth, snapMargin + 1, 0x40FFFF00);
    fill(0, scaledHeight - snapMargin, scaledWidth, scaledHeight - snapMargin + 1, 0x40FFFF00);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    ensurePanel();
    if (mouseButton == 0 && dragging == null) {
      List<HudModule> modules = HudModule.getActive();
      for (int i = modules.size() - 1; i >= 0; i--) {
        HudModule m = modules.get(i);
        int mx = m.getX();
        int my = m.getY();
        int mw = Math.max(m.getWidth(), 20);
        int mh = Math.max(m.getHeight(), 10);
        if (mouseX >= mx && mouseX <= mx + mw && mouseY >= my && mouseY <= my + mh) {
          dragging = m;
          dragOffX = mouseX - mx;
          dragOffY = mouseY - my;
          return;
        }
      }
    }
    hudPanel.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  protected void mouseReleased(int mouseX, int mouseY, int releaseButton) {
    if (releaseButton == 0 && dragging != null) {
      if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
          && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        snapToNearest(dragging);
        HudModule.layoutByCorner(HudModule.getActive(), this.width, this.height);
      }
      dragging = null;
    }
    ensurePanel();
    hudPanel.mouseReleased(mouseX, mouseY, releaseButton);
    super.mouseReleased(mouseX, mouseY, releaseButton);
  }

  private void snapToNearest(HudModule m) {
    int centerX = this.width / 2;
    int centerY = this.height / 2;
    boolean right = m.getX() + m.getWidth() / 2 > centerX;
    boolean bottom = m.getY() + m.getHeight() / 2 > centerY;
    HudModule.Corner bestCorner;
    if (right && bottom) bestCorner = HudModule.Corner.BOTTOM_RIGHT;
    else if (right) bestCorner = HudModule.Corner.TOP_RIGHT;
    else if (bottom) bestCorner = HudModule.Corner.BOTTOM_LEFT;
    else bestCorner = HudModule.Corner.TOP_LEFT;
    m.setCorner(bestCorner);
    // layoutByCorner skips manually positioned elements; clear the dragged
    // position so the new corner stack actually applies.
    m.resetPosition();
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

  private void drawHOutline(int x1, int x2, int y, int color) {
    fill(x1, y, x2 + 1, y + 1, color);
  }

  private void drawVOutline(int x, int y1, int y2, int color) {
    fill(x, y1, x + 1, y2 + 1, color);
  }
}
