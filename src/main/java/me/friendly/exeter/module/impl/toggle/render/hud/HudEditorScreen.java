package me.friendly.exeter.module.impl.toggle.render.hud;

import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.Panel;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Item;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.ModuleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class HudEditorScreen extends Screen {
  private static HudEditorScreen instance;

  private HudModule dragging;
  private int dragOffX;
  private int dragOffY;

  private Panel hudPanel;

  public HudEditorScreen() {
    super(Component.literal("HUD Editor"));
  }

  public static HudEditorScreen getInstance() {
    if (instance == null) {
      instance = new HudEditorScreen();
    }
    return instance;
  }

  /** The ClickGUI Background setting dims every fullscreen UI, the HUD editor included. */
  private static boolean isBackgroundEnabled() {
    if (Exeter.getInstance() == null) {
      return true;
    }
    var module = Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
    return !(module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui gui)
        || gui.showBackground.getValue();
  }

  private void ensurePanel() {
    if (hudPanel != null) return;

    hudPanel =
        new Panel("HUD", 50, 50, true) {
          @Override
          public void setupItems() {
            Exeter.getInstance()
                .getModuleManager()
                .getRegistry()
                .forEach(
                    module -> {
                      if (module instanceof ToggleableModule toggleable
                          && toggleable.getModuleType() == ModuleType.HUD) {
                        this.addButton(new ModuleButton(toggleable));
                      }
                    });
          }
        };
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
    RenderMethods.guiGraphics = guiGraphics;
    if (isBackgroundEnabled()) {
      RenderMethods.drawGradientRect(0.0F, 0.0F, this.width, this.height, 536870912, -1879048192);
    }

    int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
    int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

    ensurePanel();

    drawHudModules(mouseX, mouseY, scaledWidth, scaledHeight);
    drawSnapGuides(scaledWidth, scaledHeight);

    hudPanel.drawScreen(mouseX, mouseY, partialTicks);
    drawHoveredDescription(mouseX, mouseY);
  }

  /** Mirrors the ClickGUI hover text so setting descriptions work in this screen too. */
  private void drawHoveredDescription(int mouseX, int mouseY) {
    me.friendly.exeter.module.impl.toggle.render.ClickGui guiModule = null;
    if (Exeter.getInstance() != null) {
      var module = Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
      if (module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui gui) {
        if (!gui.showDescriptions.getValue()) return;
        guiModule = gui;
      }
    }
    if (hudPanel == null || !hudPanel.getOpen()) return;
    String hoveredDesc = null;
    for (Item item : hudPanel.getItems()) {
      if (!(item instanceof ModuleButton moduleButton)) continue;
      String settingDesc = moduleButton.getHoveredSettingDescription(mouseX, mouseY);
      if (settingDesc != null) {
        hoveredDesc = settingDesc;
        break;
      }
      float ix = item.getX();
      float iy = item.getY();
      if (mouseX >= ix
          && mouseX <= ix + item.getWidth()
          && mouseY >= iy
          && mouseY <= iy + item.getHeight()) {
        String desc = moduleButton.getModule().getDescription();
        if (desc != null && !desc.isEmpty()) {
          hoveredDesc = desc;
        }
      }
    }
    me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui.drawHoveredDescription(
        hoveredDesc, hudPanel, this.width, this.height, guiModule);
  }

  private void drawHudModules(int mouseX, int mouseY, int scaledWidth, int scaledHeight) {
    List<HudModule> modules = HudModule.getActive();
    HudModule.clampOnScreen(modules, scaledWidth, scaledHeight);

    for (HudModule m : modules) {
      int mx = m.getX();
      int my = m.getY();
      int mw = m.getWidth();
      int mh = m.getHeight();

      RenderMethods.drawRect(mx - 1, my - 1, mx + mw + 1, my + mh + 1, 0x80FFFFFF);
      RenderMethods.drawRect(mx, my, mx + mw, my + mh, 0x44000000);

      RenderMethods.guiGraphics.text(
          Minecraft.getInstance().font, m.getLabel(), mx + 2, my + 2, 0xFFFFFFFF);

      boolean hovered = mouseX >= mx && mouseX <= mx + mw && mouseY >= my && mouseY <= my + mh;
      if (hovered) {
        RenderMethods.drawRect(mx, my, mx + mw, my + mh, 0x30FFFFFF);
      }
    }
  }

  private void drawSnapGuides(int scaledWidth, int scaledHeight) {
    int snapMargin = 5;
    int half = scaledWidth / 2;

    RenderMethods.drawVLine(snapMargin, 0, scaledHeight, 0x40FFFF00);
    RenderMethods.drawVLine(scaledWidth - snapMargin, 0, scaledHeight, 0x40FFFF00);
    RenderMethods.drawVLine(half, 0, scaledHeight, 0x40FFFF00);
    RenderMethods.drawHLine(0, scaledWidth, snapMargin, 0x40FFFF00);
    RenderMethods.drawHLine(0, scaledWidth, scaledHeight - snapMargin, 0x40FFFF00);

    // Snap zones: dropping a box edge inside one of these snaps to its corner.
    // Radius comes from the HUD editor settings; 0 disables snapping entirely.
    int r = me.friendly.exeter.module.impl.toggle.client.HUDEditor.snapRange();
    RenderMethods.drawRect(0, 0, r, r, 0x18FFFF00);
    RenderMethods.drawRect(scaledWidth - r, 0, scaledWidth, r, 0x18FFFF00);
    RenderMethods.drawRect(0, scaledHeight - r, r, scaledHeight, 0x18FFFF00);
    RenderMethods.drawRect(
        scaledWidth - r, scaledHeight - r, scaledWidth, scaledHeight, 0x18FFFF00);
    RenderMethods.drawRect(half - r, 0, half + r, r, 0x18FFFF00);
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean bool) {
    int mouseX = (int) event.x();
    int mouseY = (int) event.y();
    int button = event.button();

    ensurePanel();
    hudPanel.mouseClicked(mouseX, mouseY, button);

    int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
    int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
    HudModule.layoutByCorner(HudModule.getActive(), sw, sh);

    List<HudModule> modules = HudModule.getActive();
    for (int i = modules.size() - 1; i >= 0; i--) {
      HudModule m = modules.get(i);
      int mx = m.getX();
      int my = m.getY();
      int mw = m.getWidth();
      int mh = m.getHeight();
      if (mouseX >= mx && mouseX <= mx + mw && mouseY >= my && mouseY <= my + mh) {
        dragging = m;
        dragOffX = mouseX - mx;
        dragOffY = mouseY - my;
        break;
      }
    }
    return super.mouseClicked(event, bool);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    ensurePanel();
    hudPanel.mouseReleased((int) event.x(), (int) event.y(), event.button());

    if (dragging != null) {
      // Shift skips snapping: exact drop position is kept as free placement.
      if (Minecraft.getInstance().hasShiftDown()) {
        dragging.setFree(true);
      } else {
        snapToNearest(dragging);
      }
      int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
      int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
      HudModule.layoutByCorner(HudModule.getActive(), sw, sh);
      dragging = null;
    }
    return super.mouseReleased(event);
  }

  @Override
  public void mouseMoved(double mouseX, double mouseY) {
    if (dragging != null) {
      int newX = (int) mouseX - dragOffX;
      int newY = (int) mouseY - dragOffY;
      dragging.setX(newX);
      dragging.setY(newY);
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollDelta) {
    ensurePanel();
    if (hudPanel.containsMouse((int) mouseX, (int) mouseY)) {
      hudPanel.scroll((int) (scrollDelta * 12));
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollDelta);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void init() {}

  /**
   * Snaps to a corner or the top center when the matching box edge lands within radius of one;
   * otherwise the module keeps its exact drop position as free placement.
   */
  private void snapToNearest(HudModule m) {
    int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
    int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
    int margin = 5;

    int x = m.getX();
    int y = m.getY();
    int w = m.getWidth();
    int h = m.getHeight();
    int cx = x + w / 2;

    // {anchorX, anchorY, refX, refY, corner}
    int[][] anchors = {
      {margin, margin, x, y},
      {scaledWidth - margin, margin, x + w, y},
      {margin, scaledHeight - margin, x, y + h},
      {scaledWidth - margin, scaledHeight - margin, x + w, y + h},
      {scaledWidth / 2, margin, cx, y}
    };
    HudModule.Corner[] corners = {
      HudModule.Corner.TOP_LEFT,
      HudModule.Corner.TOP_RIGHT,
      HudModule.Corner.BOTTOM_LEFT,
      HudModule.Corner.BOTTOM_RIGHT,
      HudModule.Corner.TOP_CENTER
    };
    HudModule.Corner best = null;
    double bestDist =
        (double) me.friendly.exeter.module.impl.toggle.client.HUDEditor.snapRange()
            * me.friendly.exeter.module.impl.toggle.client.HUDEditor.snapRange();
    for (int i = 0; i < anchors.length; i++) {
      double dx = anchors[i][2] - anchors[i][0];
      double dy = anchors[i][3] - anchors[i][1];
      double dist = dx * dx + dy * dy;
      if (dist < bestDist) {
        bestDist = dist;
        best = corners[i];
      }
    }

    if (best == null) {
      m.setFree(true);
      return;
    }
    m.setCorner(best);
    m.setFree(false);
  }
}
