package me.friendly.exeter.window.impl;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.account.Account;
import me.friendly.exeter.account.auth.SessionManager;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.window.Window;

/** Offline alt manager for beta. Betacraft auth may be added later. */
public class AccountWindow extends Window {
  private static final int ENTRY_HEIGHT = 14;
  private static final int BUTTON_HEIGHT = 12;

  private final StringBuilder inputBuffer = new StringBuilder();
  private boolean addingMode = false;
  private int selectedIndex = -1;
  private boolean cursorVisible = true;
  private long lastBlinkTime = System.currentTimeMillis();

  private volatile String status = null;
  private volatile long statusExpiry = 0;

  public AccountWindow(int x, int y, int width, int height) {
    super("Accounts", x, y, width, height);
  }

  private int barHeight() {
    return addingMode ? 34 : 18;
  }

  @Override
  protected void renderContent(int mouseX, int mouseY, float partialTicks) {
    int contentY = y + TITLE_HEIGHT;
    int listY = contentY + 14;
    int barY = y + height - barHeight();
    int listH = barY - listY;

    List<Account> accounts =
        new ArrayList<Account>(Exeter.getInstance().getAccountManager().getRegistry());
    String current = SessionManager.currentName();
    FontUtil.drawString(
        "Current: " + current + " (" + accounts.size() + ")",
        x + 5.0f,
        contentY + 2.0f,
        0xFFAAAAAA);

    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    int rowY = listY;
    for (int i = 0; i < Math.min(accounts.size(), maxVisible); i++) {
      Account account = accounts.get(i);
      boolean hovered =
          mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT;
      if (hovered || i == selectedIndex) {
        fill(x, rowY, x + width, rowY + ENTRY_HEIGHT, 0x20FFFFFF);
      }
      String name = account.getUsername();
      if (name.equals(current)) {
        name = "> " + name;
      }
      FontUtil.drawString(name, x + 5.0f, rowY + 2.0f, 0xFFEEEEEE);
      rowY += ENTRY_HEIGHT;
    }

    if (accounts.isEmpty() && !addingMode) {
      FontUtil.drawString(
          "No accounts added.", x + width / 2.0f - 40.0f, listY + listH / 2.0f, 0xFF888888);
    }

    if (status() != null) {
      String message = status();
      FontUtil.drawString(
          message, x + width / 2.0f - FontUtil.getStringWidth(message) / 2.0f,
          (float) (barY - 12), 0xFFFFFFFF);
    }

    fill(x, barY, x + width, y + height, 0x77111111);

    int btnY;
    if (addingMode) {
      int inputY = barY + 2;
      fill(x + 3, inputY, x + width - 3, inputY + 14, 0xFF222222);
      long now = System.currentTimeMillis();
      if (now - lastBlinkTime >= 500) {
        lastBlinkTime = now;
        cursorVisible = !cursorVisible;
      }
      String text = inputBuffer.toString();
      FontUtil.drawString(text + "_", x + 6.0f, inputY + 3.0f, 0xFFEEEEEE);
      btnY = barY + 18;
    } else {
      btnY = barY + 3;
    }
    drawButton(0, btnY, mouseX, mouseY, addingMode ? "Confirm" : "Login");
    drawButton(1, btnY, mouseX, mouseY, addingMode ? "Cancel" : "Add");
    drawButton(2, btnY, mouseX, mouseY, "Delete");
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
    int color;
    if (!enabled) {
      color = 0xFF333333;
    } else if (hovered) {
      color = Colors.getClientColorCustomAlpha(200);
    } else {
      color = Colors.getClientColorCustomAlpha(120);
    }
    fill(bx, btnY, bx + bw, btnY + BUTTON_HEIGHT, color);
    FontUtil.drawString(
        label,
        bx + bw / 2.0f - FontUtil.getStringWidth(label) / 2.0f,
        btnY + 2.0f,
        enabled ? 0xFFFFFFFF : 0xFF777777);
  }

  private boolean isButtonEnabled(int id) {
    if (id == 0 || id == 2) {
      return selectedIndex >= 0
          && selectedIndex < Exeter.getInstance().getAccountManager().getRegistry().size();
    }
    return true;
  }

  @Override
  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    if (button != 0) {
      return false;
    }
    int barY = y + height - barHeight();
    int btnY = addingMode ? barY + 18 : barY + 3;
    if (clickButton(0, btnY, mouseX, mouseY)) {
      if (addingMode) {
        confirmAdd();
      } else {
        loginSelected();
      }
      return true;
    }
    if (clickButton(1, btnY, mouseX, mouseY)) {
      if (addingMode) {
        addingMode = false;
        inputBuffer.setLength(0);
      } else {
        addingMode = true;
        inputBuffer.setLength(0);
        setStatus(null, 0);
      }
      return true;
    }
    if (clickButton(2, btnY, mouseX, mouseY)) {
      deleteSelected();
      return true;
    }

    int contentY = y + TITLE_HEIGHT;
    int listY = contentY + 14;
    int listH = barY - listY;
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    List<Account> accounts =
        new ArrayList<Account>(Exeter.getInstance().getAccountManager().getRegistry());
    for (int i = 0; i < Math.min(accounts.size(), maxVisible); i++) {
      int rowY = listY + i * ENTRY_HEIGHT;
      if (mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT) {
        selectedIndex = i;
        return true;
      }
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
    if (addingMode) {
      if (keyCode == 28) {
        confirmAdd();
        return true;
      }
      if (keyCode == 14) {
        if (inputBuffer.length() > 0) {
          inputBuffer.deleteCharAt(inputBuffer.length() - 1);
        }
        return true;
      }
      if (keyCode == 1) {
        addingMode = false;
        inputBuffer.setLength(0);
        return true;
      }
      if (typedChar >= 32 && typedChar < 127 && inputBuffer.length() < 16) {
        inputBuffer.append(typedChar);
        return true;
      }
      return false;
    }
    if (keyCode == 28) {
      loginSelected();
      return true;
    }
    if (keyCode == 211) {
      deleteSelected();
      return true;
    }
    return false;
  }

  private void confirmAdd() {
    String username = inputBuffer.toString().trim();
    inputBuffer.setLength(0);
    addingMode = false;
    if (username.isEmpty() || username.length() > 16) {
      setStatus("Invalid username", 3000);
      return;
    }
    for (Account account : Exeter.getInstance().getAccountManager().getRegistry()) {
      if (account.getUsername().equalsIgnoreCase(username)) {
        setStatus("Already added (" + username + ")", 3000);
        return;
      }
    }
    Exeter.getInstance().getAccountManager().register(new Account(username));
    Exeter.getInstance().getAccountManager().save();
    setStatus("Added (" + username + ")", 3000);
  }

  private void loginSelected() {
    List<Account> accounts = Exeter.getInstance().getAccountManager().getRegistry();
    if (selectedIndex < 0 || selectedIndex >= accounts.size()) {
      return;
    }
    Account account = accounts.get(selectedIndex);
    SessionManager.setOffline(account.getUsername());
    setStatus("Logged in (" + account.getUsername() + ")", 5000);
  }

  private void deleteSelected() {
    List<Account> accounts = Exeter.getInstance().getAccountManager().getRegistry();
    if (selectedIndex < 0 || selectedIndex >= accounts.size()) {
      return;
    }
    Exeter.getInstance().getAccountManager().unregister(accounts.get(selectedIndex));
    Exeter.getInstance().getAccountManager().save();
    selectedIndex = -1;
  }
}
