package me.friendly.exeter.window;

import java.util.function.IntPredicate;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.active.render.Colors;

/**
 * Shared button-row layout for windows. Columns share equal widths with the last column stretched
 * flush, matching the hand-rolled math this replaces.
 */
public final class WindowButtons {
  private final Window window;
  private final int columns;
  private final int buttonHeight;
  private final IntPredicate enabled;

  public WindowButtons(Window window, int columns, int buttonHeight, IntPredicate enabled) {
    this.window = window;
    this.columns = columns;
    this.buttonHeight = buttonHeight;
    this.enabled = enabled;
  }

  public WindowButtons(Window window, int columns, int buttonHeight) {
    this(window, columns, buttonHeight, id -> true);
  }

  public int buttonX(int id) {
    int slot = Math.floorMod(id, columns);
    return window.getX() + 3 + slot * ((window.getWidth() - 6) / columns);
  }

  public int buttonWidth(int id) {
    int slot = Math.floorMod(id, columns);
    if (slot == columns - 1) {
      return window.getX() + window.getWidth() - 3 - buttonX(id);
    }
    return (window.getWidth() - 6) / columns - 2;
  }

  public void drawButton(int id, int btnY, int mouseX, int mouseY, String label) {
    int bx = buttonX(id);
    int bw = buttonWidth(id);
    boolean hovered =
        mouseX >= bx && mouseX <= bx + bw && mouseY >= btnY && mouseY <= btnY + buttonHeight;
    boolean on = enabled.test(id);
    int color;
    if (!on) {
      color = 0xFF333333;
    } else if (Window.useRollingRainbow()) {
      int screenW = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
      int screenH = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
      color =
          Colors.rollingSample(
              Window.rollingHorizontal(),
              Window.rollingInverse(),
              bx,
              btnY,
              screenW,
              screenH,
              hovered ? 200 : 120);
    } else {
      color =
          hovered
              ? Colors.getClientColorCustomAlpha(200)
              : Colors.getClientColorCustomAlpha(120);
    }
    RenderMethods.drawRect(bx, btnY, bx + bw, btnY + buttonHeight, color);
    FontUtil.drawString(
        label,
        bx + bw / 2 - FontUtil.getStringWidth(label) / 2,
        btnY + 2,
        on ? 0xFFFFFFFF : 0xFF777777);
  }

  public boolean clickButton(int id, int btnY, int mouseX, int mouseY) {
    if (!enabled.test(id)) {
      return false;
    }
    int bx = buttonX(id);
    return mouseX >= bx
        && mouseX <= bx + buttonWidth(id)
        && mouseY >= btnY
        && mouseY <= btnY + buttonHeight;
  }
}
