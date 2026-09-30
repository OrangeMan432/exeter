package me.friendly.exeter.window.impl;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.friend.Friend;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.window.Window;

public class FriendsWindow extends Window {
  private static final int ENTRY_HEIGHT = 14;
  private static final int BUTTON_HEIGHT = 12;

  private final StringBuilder inputBuffer = new StringBuilder();
  private int selectedIndex = -1;
  private boolean cursorVisible = true;
  private long lastBlinkTime = System.currentTimeMillis();

  private volatile String status = null;
  private volatile long statusExpiry = 0;

  public FriendsWindow(int x, int y, int width, int height) {
    super("Friends", x, y, width, height);
  }

  @Override
  protected void renderContent(int mouseX, int mouseY, float partialTicks) {
    int contentY = y + TITLE_HEIGHT;
    int listY = contentY + 2;
    int barY = y + height - 34;
    int listH = barY - listY;

    List<Friend> friends = new ArrayList<Friend>(Exeter.getInstance().getFriendManager().getRegistry());
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);

    int rowY = listY;
    for (int i = 0; i < Math.min(friends.size(), maxVisible); i++) {
      Friend friend = friends.get(i);
      boolean hovered =
          mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT;
      if (hovered || i == selectedIndex) {
        fill(x, rowY, x + width, rowY + ENTRY_HEIGHT, 0x20FFFFFF);
      }
      String display = friend.getAlias() + " (" + friend.getLabel() + ")";
      FontUtil.drawString(display, x + 5.0f, rowY + 2.0f, 0xFFEEEEEE);
      rowY += ENTRY_HEIGHT;
    }

    if (friends.isEmpty()) {
      FontUtil.drawString("No friends added.", x + width / 2 - 40.0f, listY + listH / 2.0f, 0xFF888888);
    }

    if (status() != null) {
      String current = status();
      FontUtil.drawString(
          current, x + width / 2.0f - FontUtil.getStringWidth(current) / 2.0f,
          (float) (barY - 12), 0xFFFFFFFF);
    }

    fill(x, barY, x + width, y + height, 0x77111111);

    int inputY = barY + 2;
    fill(x + 3, inputY, x + width - 3, inputY + 14, 0xFF222222);
    if (inputBuffer.length() == 0 && !isFocused()) {
      FontUtil.drawString("name [alias]...", x + 6.0f, inputY + 3.0f, 0xFF666666);
    } else {
      long now = System.currentTimeMillis();
      if (now - lastBlinkTime >= 500) {
        lastBlinkTime = now;
        cursorVisible = !cursorVisible;
      }
      String text = inputBuffer.toString();
      FontUtil.drawString(text + "_", x + 6.0f, inputY + 3.0f, 0xFFEEEEEE);
    }

    int btnY = barY + 18;
    drawButton(0, btnY, mouseX, mouseY, "Add");
    drawButton(1, btnY, mouseX, mouseY, "Remove");
    drawOverrideButton(2, btnY, mouseX, mouseY);
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

  private int buttonX(int id) {
    return x + 3 + id * ((width - 6) / 3);
  }

  private int buttonWidth(int id) {
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
    fill(bx, btnY, bx + bw, btnY + BUTTON_HEIGHT, color);
    FontUtil.drawString(
        label,
        bx + bw / 2.0f - FontUtil.getStringWidth(label) / 2.0f,
        btnY + 2.0f,
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
    fill(bx, btnY, bx + bw, btnY + BUTTON_HEIGHT, color);
    FontUtil.drawString(
        label, bx + bw / 2.0f - FontUtil.getStringWidth(label) / 2.0f, btnY + 2.0f, 0xFFFFFFFF);
  }

  private boolean isButtonEnabled(int id) {
    if (id == 1) {
      return selectedIndex >= 0
          && selectedIndex < Exeter.getInstance().getFriendManager().getRegistry().size();
    }
    return true;
  }

  @Override
  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    if (button != 0) {
      return false;
    }
    int contentY = y + TITLE_HEIGHT;
    int listY = contentY + 2;
    int barY = y + height - 34;
    int listH = barY - listY;

    List<Friend> friends = new ArrayList<Friend>(Exeter.getInstance().getFriendManager().getRegistry());
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    for (int i = 0; i < Math.min(friends.size(), maxVisible); i++) {
      int rowY = listY + i * ENTRY_HEIGHT;
      if (mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT) {
        selectedIndex = i;
        return true;
      }
    }

    int btnY = barY + 18;
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
      setStatus(next ? "Override ON: friends can be attacked" : "Override OFF", 3000);
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
  protected boolean consumeKeyTyped(char typedChar, int keyCode) {
    if (keyCode == 28) {
      addFromInput();
      return true;
    }
    if (keyCode == 14) {
      if (inputBuffer.length() > 0) {
        inputBuffer.deleteCharAt(inputBuffer.length() - 1);
      }
      return true;
    }
    if (keyCode == 1) {
      inputBuffer.setLength(0);
      selectedIndex = -1;
      return true;
    }
    if (keyCode == 211) {
      removeSelected();
      return true;
    }
    if (typedChar >= 32 && typedChar < 127 && inputBuffer.length() < 48) {
      inputBuffer.append(typedChar);
      return true;
    }
    return false;
  }

  private void addFromInput() {
    String text = inputBuffer.toString().trim();
    if (text.isEmpty()) {
      return;
    }
    String[] parts = text.split("\\s+", 2);
    String username = parts[0];
    String alias = parts.length > 1 && !parts[1].trim().isEmpty() ? parts[1].trim() : username;
    if (Exeter.getInstance().getFriendManager().isFriend(username)) {
      setStatus("Already a friend (" + username + ")", 3000);
      return;
    }
    Exeter.getInstance().getFriendManager().register(new Friend(username, alias));
    Exeter.getInstance().getFriendManager().save();
    inputBuffer.setLength(0);
    selectedIndex = Exeter.getInstance().getFriendManager().getRegistry().size() - 1;
    setStatus("Added friend (" + username + ")", 3000);
  }

  private void removeSelected() {
    List<Friend> friends = Exeter.getInstance().getFriendManager().getRegistry();
    if (selectedIndex < 0 || selectedIndex >= friends.size()) {
      return;
    }
    Friend removed = friends.get(selectedIndex);
    Exeter.getInstance().getFriendManager().unregister(removed);
    Exeter.getInstance().getFriendManager().save();
    setStatus("Removed (" + removed.getLabel() + ")", 3000);
    selectedIndex = -1;
  }
}
