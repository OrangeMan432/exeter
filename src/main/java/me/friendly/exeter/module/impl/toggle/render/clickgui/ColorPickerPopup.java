package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.awt.Color;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.active.render.Colors;

/**
 * Visual color picker popout: saturation/value square plus hue strip, writing the Colors RGB
 * properties live. Styled like the list popups.
 */
public class ColorPickerPopup implements ClickPopup {

  private static final int POPUP_W = 220;
  private static final int POPUP_H = 252;
  private static final int HEADER_H = 18;
  private static final int BUTTON_AREA_H = 18;
  private static final int SV_SIZE = 150;
  private static final int SV_CELLS = 30;
  private static final int HUE_W = 16;
  private static final int HUE_CELLS = 48;

  private int popupX;
  private int popupY;
  private float hue;
  private float saturation;
  private float value;
  private boolean draggingSv;
  private boolean draggingHue;

  public ColorPickerPopup() {
    float[] hsb = currentHsb();
    hue = hsb[0] * 360f;
    saturation = hsb[1];
    value = hsb[2];
    Colors.useRgbMode();
  }

  private static float[] currentHsb() {
    int rgb = Colors.getClientColor();
    return Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
  }

  private void apply() {
    int packed = Color.HSBtoRGB(hue / 360f, saturation, value);
    Colors.setRgb((packed >> 16) & 0xFF, (packed >> 8) & 0xFF, packed & 0xFF);
  }

  private int svX() {
    return popupX + 8;
  }

  private int svY() {
    return popupY + HEADER_H + 6;
  }

  private int hueX() {
    return svX() + SV_SIZE + 8;
  }

  private int previewY() {
    return svY() + SV_SIZE + 8;
  }

  @Override
  public void render(int mouseX, int mouseY, float partialTicks, int screenW, int screenH) {
    popupX = (screenW - POPUP_W) / 2;
    popupY = (screenH - POPUP_H) / 2;

    RenderMethods.drawRect(0, 0, screenW, screenH, 0x80000000);
    RenderMethods.drawGradientRect(
        popupX,
        popupY - 1.5f,
        popupX + POPUP_W,
        popupY + HEADER_H - 6,
        Colors.getClientColorCustomAlpha(77),
        Colors.getClientColorCustomAlpha(77));
    RenderMethods.drawRect(
        popupX, popupY + HEADER_H - 6, popupX + POPUP_W, popupY + POPUP_H, 0x77000000);
    FontUtil.drawString("Pick Color", popupX + 6, popupY + 1.5f, 0xFFFFFFFF);

    int cell = SV_SIZE / SV_CELLS;
    for (int cx = 0; cx < SV_CELLS; cx++) {
      for (int cy = 0; cy < SV_CELLS; cy++) {
        float s = (cx + 0.5f) / SV_CELLS;
        float v = 1.0f - (cy + 0.5f) / SV_CELLS;
        int col = Color.HSBtoRGB(hue / 360f, s, v) | 0xFF000000;
        RenderMethods.drawRect(
            svX() + cx * cell,
            svY() + cy * cell,
            svX() + (cx + 1) * cell,
            svY() + (cy + 1) * cell,
            col);
      }
    }
    int cursorX = svX() + Math.round(saturation * (SV_SIZE - 1));
    int cursorY = svY() + Math.round((1.0f - value) * (SV_SIZE - 1));
    RenderMethods.drawRect(cursorX - 2, cursorY - 2, cursorX + 3, cursorY + 3, 0xFFFFFFFF);
    RenderMethods.drawRect(cursorX - 1, cursorY - 1, cursorX + 2, cursorY + 2, 0xFF000000);

    int stripH = SV_SIZE / HUE_CELLS;
    for (int i = 0; i < HUE_CELLS; i++) {
      float h = (i + 0.5f) / HUE_CELLS * 360f;
      int col = Color.HSBtoRGB(h / 360f, 1.0f, 1.0f) | 0xFF000000;
      RenderMethods.drawRect(
          hueX(), svY() + i * stripH, hueX() + HUE_W, svY() + (i + 1) * stripH, col);
    }
    int hueY = svY() + Math.round(hue / 360f * (SV_SIZE - 1));
    RenderMethods.drawRect(hueX() - 2, hueY - 1, hueX() + HUE_W + 2, hueY + 2, 0xFFFFFFFF);

    int picked = Color.HSBtoRGB(hue / 360f, saturation, value) | 0xFF000000;
    RenderMethods.drawRect(svX(), previewY(), svX() + SV_SIZE, previewY() + 14, picked);
    String hex =
        String.format("#%02X%02X%02X", (picked >> 16) & 0xFF, (picked >> 8) & 0xFF, picked & 0xFF);
    FontUtil.drawString(hex, hueX(), previewY() + 3, 0xFFCCCCCC);

    int doneX = popupX + POPUP_W - 59;
    int doneY = popupY + POPUP_H - BUTTON_AREA_H;
    boolean doneHover =
        mouseX >= doneX && mouseX <= doneX + 55 && mouseY >= doneY && mouseY <= doneY + 14;
    int doneColor =
        doneHover ? Colors.getClientColorCustomAlpha(180) : Colors.getClientColorCustomAlpha(120);
    RenderMethods.drawRect(doneX, doneY, doneX + 55, doneY + 14, doneColor);
    FontUtil.drawString("Done", doneX + 16, doneY + 3, 0xFFFFFFFF);
  }

  private boolean inSv(int mouseX, int mouseY) {
    return mouseX >= svX()
        && mouseX <= svX() + SV_SIZE
        && mouseY >= svY()
        && mouseY <= svY() + SV_SIZE;
  }

  private boolean inHue(int mouseX, int mouseY) {
    return mouseX >= hueX()
        && mouseX <= hueX() + HUE_W
        && mouseY >= svY()
        && mouseY <= svY() + SV_SIZE;
  }

  private void updateSv(int mouseX, int mouseY) {
    saturation = Math.max(0f, Math.min(1f, (mouseX - svX()) / (float) SV_SIZE));
    value = Math.max(0f, Math.min(1f, 1.0f - (mouseY - svY()) / (float) SV_SIZE));
    apply();
  }

  private void updateHue(int mouseY) {
    hue = Math.max(0f, Math.min(360f, (mouseY - svY()) / (float) SV_SIZE * 360f));
    apply();
  }

  @Override
  public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (mouseButton != 0) {
      return false;
    }
    int doneX = popupX + POPUP_W - 59;
    int doneY = popupY + POPUP_H - BUTTON_AREA_H;
    if (mouseX >= doneX && mouseX <= doneX + 55 && mouseY >= doneY && mouseY <= doneY + 14) {
      ClickGui.getClickGui().closePopup();
      return true;
    }
    if (inSv(mouseX, mouseY)) {
      draggingSv = true;
      updateSv(mouseX, mouseY);
      return true;
    }
    if (inHue(mouseX, mouseY)) {
      draggingHue = true;
      updateHue(mouseY);
      return true;
    }
    return mouseX >= popupX
        && mouseX <= popupX + POPUP_W
        && mouseY >= popupY
        && mouseY <= popupY + POPUP_H;
  }

  @Override
  public boolean mouseReleased(int mouseX, int mouseY, int button) {
    draggingSv = false;
    draggingHue = false;
    return false;
  }

  @Override
  public boolean mouseDragged(int mouseX, int mouseY) {
    if (draggingSv) {
      updateSv(mouseX, mouseY);
      return true;
    }
    if (draggingHue) {
      updateHue(mouseY);
      return true;
    }
    return false;
  }

  @Override
  public boolean mouseScrolled(double scrollDelta) {
    return false;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    return false;
  }
}
