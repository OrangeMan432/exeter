package me.friendly.exeter.window;

import com.mojang.blaze3d.platform.InputConstants;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.active.render.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

public abstract class Window {
  protected String title;
  protected int x, y, width, height;
  private boolean dragging;
  private int dragOffsetX, dragOffsetY;
  private boolean focused;
  private boolean hidden;
  protected static final int TITLE_HEIGHT = 16;
  private static final int CLOSE_BUTTON_SIZE = 10;

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

    int headerColor =
        focused ? Colors.getClientColorCustomAlpha(220) : Colors.getClientColorCustomAlpha(150);
    int headerColorEnd =
        focused
            ? Colors.getDarkerClientColorCustomAlpha(220)
            : Colors.getDarkerClientColorCustomAlpha(150);

    if (useGradient()) {
      RenderMethods.drawGradientRect(
          x, y, x + width, y + TITLE_HEIGHT, headerColor, headerColorEnd);
    } else {
      RenderMethods.drawRect(x, y, x + width, y + TITLE_HEIGHT, headerColor);
    }

    FontUtil.drawString(title, x + 4, y + 4, 0xFFFFFFFF);

    int closeX = x + width - CLOSE_BUTTON_SIZE - 3;
    int closeY = y + 3;
    boolean closeHovered =
        mouseX >= closeX
            && mouseX <= closeX + CLOSE_BUTTON_SIZE
            && mouseY >= closeY
            && mouseY <= closeY + CLOSE_BUTTON_SIZE;
    int closeColor = closeHovered ? 0xFFFF5555 : 0xFFCCCCCC;
    FontUtil.drawString("X", closeX + 2, closeY + 1, closeColor);

    RenderMethods.drawRect(x, y + TITLE_HEIGHT, x + width, y + height, 0x77000000);

    renderContent(mouseX, mouseY, partialTicks);

    if (focused) {
      int accent = Colors.getClientColorCustomAlpha(220);
      RenderMethods.drawRect(x - 1, y - 1, x + width + 1, y, accent);
      RenderMethods.drawRect(x - 1, y + height, x + width + 1, y + height + 1, accent);
      RenderMethods.drawRect(x - 1, y, x, y + height, accent);
      RenderMethods.drawRect(x + width, y, x + width + 1, y + height, accent);
    }
  }

  protected abstract void renderContent(int mouseX, int mouseY, float partialTicks);

  /** Mirrors the ClickGUI {@code Gradient} setting: no gradient anywhere when it is off. */
  protected static boolean useGradient() {
    try {
      var module = Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
      return !(module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui cg)
          || cg.showGradient.getValue();
    } catch (Exception e) {
      return true;
    }
  }

  public boolean mouseClicked(int mouseX, int mouseY, int button) {
    if (hidden) return false;

    if (button == InputConstants.MOUSE_BUTTON_LEFT && isCloseHovered(mouseX, mouseY)) {
      hidden = true;
      return true;
    }

    if (button == InputConstants.MOUSE_BUTTON_LEFT && isHoveredTitle(mouseX, mouseY)) {
      dragging = true;
      dragOffsetX = mouseX - x;
      dragOffsetY = mouseY - y;
      focused = true;
      return true;
    }

    if (button == InputConstants.MOUSE_BUTTON_LEFT && isHovered(mouseX, mouseY)) {
      focused = true;
      return consumeClick(mouseX, mouseY, button);
    }

    return false;
  }

  public void mouseReleased(int button) {
    if (button == InputConstants.MOUSE_BUTTON_LEFT) {
      dragging = false;
    }
  }

  public boolean mouseDragged(int mouseX, int mouseY) {
    if (hidden || !focused) return false;
    return consumeDragged(mouseX, mouseY);
  }

  protected boolean consumeDragged(int mouseX, int mouseY) {
    return false;
  }

  public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
    if (hidden || !isHovered(mouseX, mouseY)) return false;
    return consumeScroll(mouseX, mouseY, scrollDelta);
  }

  public boolean keyPressed(KeyEvent event) {
    if (hidden || !focused) return false;
    return consumeKeyPress(event);
  }

  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    return false;
  }

  protected boolean consumeScroll(double mouseX, double mouseY, double scrollDelta) {
    return false;
  }

  protected boolean consumeKeyPress(KeyEvent event) {
    return false;
  }

  /**
   * Appends the character for a key press to a text buffer.
   *
   * <p>Must use {@code event.key()} (HID usage codes, contiguous per group), never {@code
   * event.keycode()} (the scancode). Letters map shift-aware, digits map 1-9/0 in HID order, and
   * Shift+Minus yields an underscore for Minecraft usernames.
   *
   * @return true if a character was appended
   */
  protected static boolean appendKeyChar(StringBuilder buffer, KeyEvent event, int maxLength) {
    if (buffer.length() >= maxLength) {
      return false;
    }
    int code = event.key();
    if (code >= InputConstants.KEY_A && code <= InputConstants.KEY_Z) {
      char typed = (char) ('A' + code - InputConstants.KEY_A);
      if (!Minecraft.getInstance().hasShiftDown()) {
        typed = Character.toLowerCase(typed);
      }
      buffer.append(typed);
      return true;
    }
    if (code >= InputConstants.KEY_1 && code <= InputConstants.KEY_9) {
      buffer.append((char) ('1' + code - InputConstants.KEY_1));
      return true;
    }
    if (code == InputConstants.KEY_0) {
      buffer.append('0');
      return true;
    }
    if (code == InputConstants.KEY_MINUS) {
      buffer.append(Minecraft.getInstance().hasShiftDown() ? '_' : '-');
      return true;
    }
    if (code == InputConstants.KEY_SPACE) {
      buffer.append(' ');
      return true;
    }
    return false;
  }

  public boolean isHovered(double mouseX, double mouseY) {
    return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
  }

  private boolean isHoveredTitle(double mouseX, double mouseY) {
    return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + TITLE_HEIGHT;
  }

  private boolean isCloseHovered(double mouseX, double mouseY) {
    int closeX = x + width - CLOSE_BUTTON_SIZE - 3;
    int closeY = y + 3;
    return mouseX >= closeX
        && mouseX <= closeX + CLOSE_BUTTON_SIZE
        && mouseY >= closeY
        && mouseY <= closeY + CLOSE_BUTTON_SIZE;
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

  /**
   * Modal windows (e.g. an embedded color picker) claim clicks anywhere on screen while open, since
   * their content usually extends past the window bounds.
   */
  public boolean isModal() {
    return false;
  }

  public void setHidden(boolean hidden) {
    this.hidden = hidden;
  }

  public boolean isDragging() {
    return dragging;
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

  public void setPosition(int x, int y) {
    this.x = x;
    this.y = y;
  }
}
