package me.friendly.exeter.module.impl.toggle.render.hud;

import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.clickgui.Panel;
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
    RenderMethods.drawGradientRect(0.0F, 0.0F, this.width, this.height, 536870912, -1879048192);

    int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
    int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

    ensurePanel();

    drawHudModules(mouseX, mouseY, scaledWidth, scaledHeight);
    drawSnapGuides(scaledWidth, scaledHeight);

    hudPanel.drawScreen(mouseX, mouseY, partialTicks);
  }

  private void drawHudModules(int mouseX, int mouseY, int scaledWidth, int scaledHeight) {
    List<HudModule> modules = HudModule.getActive();

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
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean bool) {
    int mouseX = (int) event.x();
    int mouseY = (int) event.y();
    int button = event.button();

    ensurePanel();
    hudPanel.mouseClicked(mouseX, mouseY, button);

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
      if (!Minecraft.getInstance().hasShiftDown()) {
        snapToNearest(dragging);
      }
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

  private void snapToNearest(HudModule m) {
    int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
    int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
    int margin = 5;

    int snapLeft = margin;
    int snapCenter = scaledWidth / 2 - m.getWidth() / 2;
    int snapRight = scaledWidth - m.getWidth() - margin;
    int snapTop = margin;
    int snapBottom = scaledHeight - m.getHeight() - margin;

    int bestDist = Integer.MAX_VALUE;
    HudModule.Corner bestCorner = m.getCorner();

    int[] xs = {snapLeft, snapCenter, snapRight};
    int[] ys = {snapTop, snapBottom};
    HudModule.Corner[] corners = {HudModule.Corner.TOP_LEFT, HudModule.Corner.BOTTOM_LEFT, HudModule.Corner.TOP_RIGHT, HudModule.Corner.BOTTOM_RIGHT};
    int ci = 0;
    for (int y : ys) {
      for (int x : xs) {
        int dist = Math.abs(m.getX() - x) + Math.abs(m.getY() - y);
        if (dist < bestDist) {
          bestDist = dist;
          bestCorner = corners[ci];
        }
      }
      ci++;
    }
    m.setCorner(bestCorner);
  }
}
