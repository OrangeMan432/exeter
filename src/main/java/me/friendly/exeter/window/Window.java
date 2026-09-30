package me.friendly.exeter.window;

import me.friendly.api.minecraft.render.font.FontUtil;
import net.minecraft.client.gui.DrawableHelper;

public abstract class Window extends DrawableHelper {
  protected String title;
  protected int x, y, width, height;
  private boolean dragging;
  private int dragOffsetX, dragOffsetY;
  private boolean focused;
  private boolean hidden;
  protected static final int TITLE_HEIGHT = 16;

  public Window(String title, int x, int y, int width, int height) {
    this.title = title;
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  public void render(int mouseX, int mouseY, float partialTicks) {
    if (hidden) return;

    if (dragging) {
      x = mouseX - dragOffsetX;
      y = mouseY - dragOffsetY;
    }

    fill(x, y, x + width, y + TITLE_HEIGHT, 0xDD222222);
    FontUtil.drawString(title, x + 4.0f, y + 4.0f, 0xFFFFFFFF);

    int closeX = x + width - 13;
    int closeY = y + 3;
    boolean closeHovered =
        mouseX >= closeX && mouseX <= closeX + 10 && mouseY >= closeY && mouseY <= closeY + 10;
    FontUtil.drawString("X", closeX + 2.0f, closeY + 1.0f, closeHovered ? 0xFFFF5555 : 0xFFCCCCCC);

    fill(x, y + TITLE_HEIGHT, x + width, y + height, 0x77000000);

    renderContent(mouseX, mouseY, partialTicks);

    if (focused) {
      int accent = 0xFFAA2222;
      fill(x - 1, y - 1, x + width + 1, y, accent);
      fill(x - 1, y + height, x + width + 1, y + height + 1, accent);
      fill(x - 1, y, x, y + height, accent);
      fill(x + width, y, x + width + 1, y + height, accent);
    }
  }

  protected abstract void renderContent(int mouseX, int mouseY, float partialTicks);

  public boolean mouseClicked(int mouseX, int mouseY, int button) {
    if (hidden) return false;

    if (button == 0 && isCloseHovered(mouseX, mouseY)) {
      hidden = true;
      return true;
    }

    if (button == 0 && isHoveredTitle(mouseX, mouseY)) {
      dragging = true;
      dragOffsetX = mouseX - x;
      dragOffsetY = mouseY - y;
      focused = true;
      return true;
    }

    if (button == 0 && isHovered(mouseX, mouseY)) {
      focused = true;
      return consumeClick(mouseX, mouseY, button);
    }

    return false;
  }

  public void mouseReleased(int button) {
    if (button == 0) {
      dragging = false;
    }
  }

  public boolean keyTyped(char typedChar, int keyCode) {
    if (hidden || !focused) return false;
    return consumeKeyTyped(typedChar, keyCode);
  }

  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    return false;
  }

  protected boolean consumeKeyTyped(char typedChar, int keyCode) {
    return false;
  }

  public boolean isHovered(int mouseX, int mouseY) {
    return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
  }

  private boolean isHoveredTitle(int mouseX, int mouseY) {
    return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + TITLE_HEIGHT;
  }

  private boolean isCloseHovered(int mouseX, int mouseY) {
    int closeX = x + width - 13;
    int closeY = y + 3;
    return mouseX >= closeX && mouseX <= closeX + 10 && mouseY >= closeY && mouseY <= closeY + 10;
  }

  public void setFocused(boolean focused) {
    this.focused = focused;
  }

  public boolean isFocused() {
    return focused;
  }

  public boolean isHidden() {
    return hidden;
  }

  public void setHidden(boolean hidden) {
    this.hidden = hidden;
  }

  public String getTitle() {
    return title;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }
}
