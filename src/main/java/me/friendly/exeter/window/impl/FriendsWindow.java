package me.friendly.exeter.window.impl;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.friend.Friend;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.window.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

/**
 * Friends list window.
 *
 * <p>Selection behaves like the account manager (click to select, Delete to remove) and the text
 * entry behaves like the console window (always-on input line with cursor, paste support). Type
 * {@code name} or {@code name alias}, then Enter or Add.
 */
public class FriendsWindow extends Window {
  private static final int ENTRY_HEIGHT = 14;
  private static final int BUTTON_HEIGHT = 12;
  private static final int INPUT_HEIGHT = 14;
  private static final int BOTTOM_BAR_HEIGHT = 30;

  private final StringBuilder inputBuffer = new StringBuilder();
  private int selectedIndex = -1;
  private int scrollOffset = 0;
  private boolean cursorVisible = true;
  private long lastBlinkTime = System.currentTimeMillis();

  private volatile String status = null;
  private volatile long statusExpiry = 0;

  public FriendsWindow(int x, int y, int width, int height) {
    super("Friends", x, y, width, height);
  }

  // Rendering

  @Override
  protected void renderContent(int mouseX, int mouseY, float partialTicks) {
    int contentY = y + TITLE_HEIGHT;
    int listY = contentY + 2;
    int barY = y + height - BOTTOM_BAR_HEIGHT;
    int listH = barY - listY;

    List<Friend> friends = new ArrayList<>(Exeter.getInstance().getFriendManager().getRegistry());
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    scrollOffset = Math.min(scrollOffset, Math.max(0, friends.size() - maxVisible));

    int rowY = listY;
    for (int i = scrollOffset; i < Math.min(friends.size(), scrollOffset + maxVisible); i++) {
      Friend friend = friends.get(i);
      boolean hovered =
          mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT;
      if (hovered || i == selectedIndex) {
        RenderMethods.drawRect(x, rowY, x + width, rowY + ENTRY_HEIGHT, 0x20FFFFFF);
      }
      String display = friend.getAlias() + " §8(" + friend.getLabel() + "§8)";
      FontUtil.drawString(display, x + 5, rowY + 2, 0xFFEEEEEE);
      rowY += ENTRY_HEIGHT;
    }

    if (friends.isEmpty()) {
      FontUtil.drawString("No friends added.", x + width / 2 - 40, listY + listH / 2, 0xFF888888);
    }

    drawStatus(listY + listH - 12);

    RenderMethods.drawRect(x, barY, x + width, y + height, 0x77111111);

    int inputY = barY + 2;
    RenderMethods.drawRect(x + 3, inputY, x + width - 3, inputY + INPUT_HEIGHT, 0xFF222222);
    if (inputBuffer.length() == 0 && !isFocused()) {
      FontUtil.drawString("name [alias]...", x + 6, inputY + 3, 0xFF666666);
    } else {
      long now = System.currentTimeMillis();
      if (now - lastBlinkTime >= 500) {
        lastBlinkTime = now;
        cursorVisible = !cursorVisible;
      }
      String text = inputBuffer.toString();
      int maxChars = 40;
      String shown =
          text.length() > maxChars ? "..." + text.substring(text.length() - maxChars) : text;
      FontUtil.drawString(shown, x + 6, inputY + 3, 0xFFEEEEEE);
      if (cursorVisible && isFocused()) {
        int cursorX = x + 6 + FontUtil.getStringWidth(shown);
        RenderMethods.drawRect(cursorX, inputY + 2, cursorX + 1, inputY + 12, 0xFFEEEEEE);
      }
    }

    int btnY = barY + INPUT_HEIGHT + 2;
    drawButton(0, btnY, mouseX, mouseY, "Add");
    drawButton(1, btnY, mouseX, mouseY, "Remove");
    drawOverrideButton(2, btnY, mouseX, mouseY);
  }

  private void drawStatus(int statusY) {
    String current = status();
    if (current != null) {
      FontUtil.drawString(
          current, x + width / 2 - FontUtil.getStringWidth(current) / 2, statusY, 0xFFFFFFFF);
    }
  }

  private String status() {
    if (status == null) {
      return null;
    }
    if (statusExpiry != 0 && System.currentTimeMillis() > statusExpiry) {
      status = null;
      return null;
    }
    return status;
  }

  private void setStatus(String message, long durationMs) {
    status = message;
    statusExpiry = durationMs <= 0 ? 0 : System.currentTimeMillis() + durationMs;
  }

  // Buttons: three equal columns.

  private int buttonX(int id) {
    return x + 3 + id * ((width - 6) / 3);
  }

  private int buttonWidth(int id) {
    // Last column stretches so its right edge sits flush with the text entry box.
    if (id == 2) {
      return x + width - 3 - buttonX(id);
    }
    return (width - 6) / 3 - 2;
  }

  private void drawButton(int id, int btnY, int mouseX, int mouseY, String label) {
    int bx = buttonX(id);
    int bw = buttonWidth(id);
    boolean hovered =
        mouseX >= bx && mouseX <= bx + bw && mouseY >= btnY && mouseY <= btnY + BUTTON_HEIGHT;
    boolean enabled = isButtonEnabled(id);
    int color =
        !enabled
            ? 0xFF333333
            : hovered
                ? Colors.getClientColorCustomAlpha(200)
                : Colors.getClientColorCustomAlpha(120);
    RenderMethods.drawRect(bx, btnY, bx + bw, btnY + BUTTON_HEIGHT, color);
    FontUtil.drawString(
        label,
        bx + bw / 2 - FontUtil.getStringWidth(label) / 2,
        btnY + 2,
        enabled ? 0xFFFFFFFF : 0xFF777777);
  }

  private void drawOverrideButton(int id, int btnY, int mouseX, int mouseY) {
    boolean override = Exeter.getInstance().getFriendManager().isOverride();
    int bx = buttonX(id);
    int bw = buttonWidth(id);
    boolean hovered =
        mouseX >= bx && mouseX <= bx + bw && mouseY >= btnY && mouseY <= btnY + BUTTON_HEIGHT;
    String label = "Override: " + (override ? "ON" : "OFF");
    int color =
        override
            ? (hovered ? 0xFFDD3333 : 0xFF992222)
            : (hovered
                ? Colors.getClientColorCustomAlpha(200)
                : Colors.getClientColorCustomAlpha(120));
    RenderMethods.drawRect(bx, btnY, bx + bw, btnY + BUTTON_HEIGHT, color);
    FontUtil.drawString(
        label, bx + bw / 2 - FontUtil.getStringWidth(label) / 2, btnY + 2, 0xFFFFFFFF);
  }

  private boolean isButtonEnabled(int id) {
    if (id == 1) {
      return selectedIndex >= 0
          && selectedIndex < Exeter.getInstance().getFriendManager().getRegistry().size();
    }
    return true;
  }

  // Input

  @Override
  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    if (button != InputConstants.MOUSE_BUTTON_LEFT) {
      return false;
    }
    int contentY = y + TITLE_HEIGHT;
    int listY = contentY + 2;
    int barY = y + height - BOTTOM_BAR_HEIGHT;
    int listH = barY - listY;

    List<Friend> friends = new ArrayList<>(Exeter.getInstance().getFriendManager().getRegistry());
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    for (int i = scrollOffset; i < Math.min(friends.size(), scrollOffset + maxVisible); i++) {
      int rowY = listY + (i - scrollOffset) * ENTRY_HEIGHT;
      if (mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT) {
        selectedIndex = i;
        return true;
      }
    }

    int btnY = barY + INPUT_HEIGHT + 2;
    if (clickButton(0, btnY, mouseX, mouseY)) {
      addFromInput();
      return true;
    }
    if (clickButton(1, btnY, mouseX, mouseY)) {
      removeSelected();
      return true;
    }
    if (clickButton(2, btnY, mouseX, mouseY)) {
      boolean next = !Exeter.getInstance().getFriendManager().isOverride();
      Exeter.getInstance().getFriendManager().setOverride(next);
      setStatus(next ? "§cOverride ON: friends can be attacked" : "§aOverride OFF", 3000);
      return true;
    }
    return true;
  }

  private boolean clickButton(int id, int btnY, int mouseX, int mouseY) {
    if (!isButtonEnabled(id)) {
      return false;
    }
    int bx = buttonX(id);
    return mouseX >= bx
        && mouseX <= bx + buttonWidth(id)
        && mouseY >= btnY
        && mouseY <= btnY + BUTTON_HEIGHT;
  }

  @Override
  protected boolean consumeScroll(double mouseX, double mouseY, double scrollDelta) {
    List<Friend> friends = Exeter.getInstance().getFriendManager().getRegistry();
    int listH = (y + height - BOTTOM_BAR_HEIGHT) - (y + TITLE_HEIGHT + 2);
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    int maxScroll = Math.max(0, friends.size() - maxVisible);
    if (scrollDelta < 0) {
      scrollOffset = Math.min(maxScroll, scrollOffset + 1);
    } else {
      scrollOffset = Math.max(0, scrollOffset - 1);
    }
    return true;
  }

  @Override
  protected boolean consumeKeyPress(KeyEvent event) {
    int key = event.key();

    if (key == InputConstants.KEY_RETURN) {
      addFromInput();
      return true;
    }
    if (key == InputConstants.KEY_BACKSPACE) {
      if (inputBuffer.length() > 0) {
        inputBuffer.deleteCharAt(inputBuffer.length() - 1);
      }
      return true;
    }
    if (key == InputConstants.KEY_ESCAPE) {
      inputBuffer.setLength(0);
      selectedIndex = -1;
      return true;
    }
    if (key == InputConstants.KEY_DELETE) {
      removeSelected();
      return true;
    }
    if (key == InputConstants.KEY_V && Minecraft.getInstance().hasControlDown()) {
      String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
      if (clip != null) {
        inputBuffer.append(clip.trim());
      }
      return true;
    }

    return appendKeyChar(inputBuffer, event, 48);
  }

  // Actions

  private void addFromInput() {
    String text = inputBuffer.toString().trim();
    if (text.isEmpty()) {
      return;
    }
    String[] parts = text.split("\\s+", 2);
    String username = parts[0];
    String alias = parts.length > 1 && !parts[1].isBlank() ? parts[1] : username;
    if (Exeter.getInstance().getFriendManager().isFriend(username)) {
      setStatus("§cAlready a friend (" + username + ")", 3000);
      return;
    }
    Exeter.getInstance().getFriendManager().register(new Friend(username, alias));
    Exeter.getInstance().getFriendManager().save();
    inputBuffer.setLength(0);
    selectedIndex = Exeter.getInstance().getFriendManager().getRegistry().size() - 1;
    setStatus("§aAdded friend (" + username + ")", 3000);
  }

  private void removeSelected() {
    List<Friend> friends = Exeter.getInstance().getFriendManager().getRegistry();
    if (selectedIndex < 0 || selectedIndex >= friends.size()) {
      return;
    }
    Friend removed = friends.get(selectedIndex);
    Exeter.getInstance().getFriendManager().unregister(removed);
    Exeter.getInstance().getFriendManager().save();
    setStatus("§7Removed (" + removed.getLabel() + ")", 3000);
    selectedIndex = -1;
  }
}
