package me.larp.client.module.impl.toggle.render.hud;

import java.util.ArrayList;
import java.util.List;
import me.larp.api.minecraft.render.RenderMethods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HudEditorScreen extends Screen {
  private static HudEditorScreen instance;
  private final List<HudComponent> components = new ArrayList<>();
  private HudComponent dragging;
  private int dragOffX;
  private int dragOffY;

  public HudEditorScreen() {
    super(Component.literal("HUD Editor"));
  }

  public static HudEditorScreen getInstance() {
    if (instance == null) {
      instance = new HudEditorScreen();
    }
    return instance;
  }

  public void setComponents(List<HudComponent> comps) {
    components.clear();
    components.addAll(comps);
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
    RenderMethods.guiGraphics = guiGraphics;
    RenderMethods.drawGradientRect(0.0F, 0.0F, this.width, this.height, 536870912, -1879048192);

    int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
    int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

    for (HudComponent comp : components) {
      if (!comp.isVisible()) continue;

      int cx = comp.getX();
      int cy = comp.getY();
      int cw = comp.getWidth();
      int ch = comp.getHeight();

      RenderMethods.drawRect(cx - 1, cy - 1, cx + cw + 1, cy + ch + 1, 0x80FFFFFF);
      RenderMethods.drawRect(cx, cy, cx + cw, cy + ch, 0x44000000);

      RenderMethods.guiGraphics.text(
          Minecraft.getInstance().font, comp.getLabel(), cx + 2, cy + 2, 0xFFFFFFFF);

      boolean hovered = mouseX >= cx && mouseX <= cx + cw && mouseY >= cy && mouseY <= cy + ch;
      if (hovered) {
        RenderMethods.drawRect(cx, cy, cx + cw, cy + ch, 0x30FFFFFF);
      }
    }

    renderSnapGuides(scaledWidth, scaledHeight);
  }

  private void renderSnapGuides(int scaledWidth, int scaledHeight) {
    int snapMargin = 5;
    int half = scaledWidth / 2;

    RenderMethods.drawVLine(snapMargin, 0, scaledHeight, 0x40FFFF00);
    RenderMethods.drawVLine(scaledWidth - snapMargin, 0, scaledHeight, 0x40FFFF00);
    RenderMethods.drawVLine(half, 0, scaledHeight, 0x40FFFF00);
    RenderMethods.drawHLine(0, scaledWidth, snapMargin, 0x40FFFF00);
    RenderMethods.drawHLine(0, scaledWidth, scaledHeight - snapMargin, 0x40FFFF00);
  }

  @Override
  public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean bool) {
    int mouseX = (int) event.x();
    int mouseY = (int) event.y();

    for (int i = components.size() - 1; i >= 0; i--) {
      HudComponent comp = components.get(i);
      if (!comp.isVisible()) continue;
      int cx = comp.getX();
      int cy = comp.getY();
      int cw = comp.getWidth();
      int ch = comp.getHeight();

      if (mouseX >= cx && mouseX <= cx + cw && mouseY >= cy && mouseY <= cy + ch) {
        dragging = comp;
        dragOffX = mouseX - cx;
        dragOffY = mouseY - cy;
        break;
      }
    }
    return super.mouseClicked(event, bool);
  }

  @Override
  public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
    if (dragging != null) {
      int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
      int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

      if (!Minecraft.getInstance().hasShiftDown()) {
        int sx = dragging.getSnapX(scaledWidth, scaledHeight);
        int sy = dragging.getSnapY(scaledWidth, scaledHeight);
        dragging.setX(sx);
        dragging.setY(sy);
        applyStacking(dragging, scaledWidth, scaledHeight);
      }
      dragging = null;
    }
    return super.mouseReleased(event);
  }

  private void applyStacking(HudComponent comp, int sw, int sh) {
    int snapMargin = 5;
    int gap = 2;
    int sx = comp.getX();
    int sy = comp.getY();
    int ch = comp.getHeight();

    boolean top = sy == snapMargin;
    boolean bottom = !top && sy == sh - ch - snapMargin;
    if (!top && !bottom) return;

    int totalOffset = 0;
    for (HudComponent other : components) {
      if (other == comp || !other.isVisible()) continue;
      if (other.getSnapX(sw, sh) != sx) continue;
      int otherSy = other.getSnapY(sw, sh);
      boolean otherTop = otherSy == snapMargin;
      boolean otherBottom = !otherTop;
      if (top && !otherTop) continue;
      if (bottom && !otherBottom) continue;
      totalOffset += other.getHeight() + gap;
    }

    if (totalOffset > 0) {
      comp.setY(top ? sy + totalOffset : sy - totalOffset);
    }
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
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void init() {}
}
