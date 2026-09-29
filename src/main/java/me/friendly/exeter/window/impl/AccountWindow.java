package me.friendly.exeter.window.impl;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.account.Account;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.window.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

/**
 * Lists stored offline usernames and lets one be added or removed.
 *
 * <p>The Microsoft OAuth, device-code browser flow, raw session token and refresh-token paths were
 * removed. Each of them required outbound HTTPS to login.microsoftonline.com or Mojang, and those
 * APIs return no CORS headers, so a browser build cannot use them. Session switching went with
 * them: the client no longer writes the game's final {@code User} field, so the active session is
 * whatever the host game started with.
 *
 * <p>Ban state still shows, because it is read from disconnect packets rather than from an auth
 * call.
 *
 * <p>Originally based on OpenMyau's account manager windows (derived from
 * https://github.com/ksyzov/AccountManager, originally LGPL, modified version GPL v3).
 */
public class AccountWindow extends Window {
  private static final String TAG = "AccountManager";
  private static final int ENTRY_HEIGHT = 14;
  private static final int BUTTON_HEIGHT = 12;
  private static final int BUTTON_COUNT = 3;

  private enum Mode {
    LIST,
    OFFLINE
  }

  private volatile Mode mode = Mode.LIST;
  private final StringBuilder inputBuffer = new StringBuilder();

  private volatile int selectedAccount = -1;
  private int scrollOffset = 0;

  private volatile String status = null;
  private volatile long statusExpiry = 0;

  public AccountWindow(int x, int y, int width, int height) {
    super("Accounts", x, y, width, height);
  }

  // Rendering

  @Override
  protected void renderContent(int mouseX, int mouseY, float partialTicks) {
    int contentY = y + TITLE_HEIGHT;
    if (mode == Mode.LIST) {
      renderList(mouseX, mouseY, contentY);
    } else {
      renderInput(mouseX, mouseY, contentY, "Enter offline username");
    }
  }

  private void renderList(int mouseX, int mouseY, int contentY) {
    List<Account> accounts = accounts();
    String current =
        Minecraft.getInstance().getUser() != null
            ? Minecraft.getInstance().getUser().getName()
            : "";
    FontUtil.drawString(
        "§7Session: §3" + current + " §8(" + accounts.size() + ")",
        x + 5,
        contentY + 3,
        0xFFFFFFFF);

    int listY = contentY + 16;
    int listHeight = height - TITLE_HEIGHT - 16 - 30;
    int maxVisible = Math.max(1, listHeight / ENTRY_HEIGHT);
    scrollOffset = Math.min(scrollOffset, Math.max(0, accounts.size() - maxVisible));

    int rowY = listY + 2;
    for (int i = scrollOffset; i < Math.min(accounts.size(), scrollOffset + maxVisible); i++) {
      Account account = accounts.get(i);
      boolean hovered =
          mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT;
      if (hovered || i == selectedAccount) {
        RenderMethods.drawRect(x, rowY, x + width, rowY + ENTRY_HEIGHT, 0x20FFFFFF);
      }

      String name = account.getUsername();
      if (name == null || name.isBlank()) {
        name = "§7§l?";
      } else if (name.equals(current)) {
        name = "§a§l" + name;
      } else {
        name = "§f" + name;
      }
      FontUtil.drawString(name, x + 5, rowY + 2, 0xFFFFFFFF);

      String ban = banText(account);
      FontUtil.drawString(ban, x + width - 5 - FontUtil.getStringWidth(ban), rowY + 2, 0xFFFFFFFF);

      rowY += ENTRY_HEIGHT;
    }

    if (accounts.isEmpty()) {
      FontUtil.drawString(
          "No usernames stored.",
          x + width / 2 - FontUtil.getStringWidth("No usernames stored.") / 2,
          listY + listHeight / 2,
          0xFF888888);
    }

    int barY = contentY + height - TITLE_HEIGHT - 28;
    RenderMethods.drawRect(x, barY, x + width, y + height, 0x77111111);
    drawButton(0, barY + 3, mouseX, mouseY, "Add");
    drawButton(1, barY + 3, mouseX, mouseY, "Delete");
    drawStatus(barY - 14);
  }

  private void renderInput(int mouseX, int mouseY, int contentY, String title) {
    FontUtil.drawString(
        title, x + width / 2 - FontUtil.getStringWidth(title) / 2, contentY + 30, 0xFFAAAAAA);
    int fieldX = x + 10;
    int fieldY = contentY + 48;
    RenderMethods.drawRect(fieldX, fieldY, x + width - 10, fieldY + 14, 0xFF222222);
    String text = inputBuffer.toString();
    int maxChars = 16;
    String shown =
        text.length() > maxChars ? "..." + text.substring(text.length() - maxChars) : text;
    FontUtil.drawString(shown + "_", fieldX + 3, fieldY + 3, 0xFFEEEEEE);
    FontUtil.drawString("Enter = confirm, Esc clears", x + width / 2 - 75, fieldY + 20, 0xFF888888);
    String current = status();
    if (current != null) {
      FontUtil.drawString(
          current, x + width / 2 - FontUtil.getStringWidth(current) / 2, fieldY + 34, 0xFFFFFFFF);
    }
    int barY = contentY + height - TITLE_HEIGHT - 28;
    RenderMethods.drawRect(x, barY, x + width, y + height, 0x77111111);
    drawButton(2, barY + 3, mouseX, mouseY, "Confirm");
    drawButton(0, barY + 3, mouseX, mouseY, "Cancel");
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

  private String banText(Account account) {
    long now = System.currentTimeMillis();
    long unban = account.getUnban();
    if (unban < 0L) {
      return "§4§lBANNED";
    }
    if (unban <= now) {
      return "§2§lOK";
    }
    long diff = unban - now;
    long s = (diff / 1000L) % 60L;
    long m = (diff / 60000L) % 60L;
    long h = (diff / 3600000L) % 24L;
    long d = (diff / 86400000L);
    String remaining =
        ((d > 0L ? d + "d" : "")
                + (h > 0L ? " " + h + "h" : "")
                + (m > 0L ? " " + m + "m" : "")
                + (s > 0L ? " " + s + "s" : ""))
            .trim();
    return "§c" + remaining + " §c§l!";
  }

  // Buttons: equal columns on a single row.

  private int buttonX(int id) {
    int slot = id % BUTTON_COUNT;
    return x + 3 + slot * ((width - 6) / BUTTON_COUNT);
  }

  private int buttonWidth() {
    return (width - 6) / BUTTON_COUNT - 2;
  }

  private void drawButton(int id, int btnY, int mouseX, int mouseY, String label) {
    int bx = buttonX(id);
    int bw = buttonWidth();
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

  private boolean isButtonEnabled(int id) {
    return switch (id) {
      case 1 -> mode == Mode.LIST && selectedAccount >= 0 && selectedAccount < accounts().size();
      default -> true;
    };
  }

  // Input

  @Override
  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    if (button != InputConstants.MOUSE_BUTTON_LEFT) {
      return false;
    }
    int contentY = y + TITLE_HEIGHT;
    int barY = contentY + height - TITLE_HEIGHT - 28;

    if (mode == Mode.LIST) {
      int listY = contentY + 16;
      int listHeight = height - TITLE_HEIGHT - 16 - 30;
      int maxVisible = Math.max(1, listHeight / ENTRY_HEIGHT);
      List<Account> accounts = accounts();

      for (int i = scrollOffset; i < Math.min(accounts.size(), scrollOffset + maxVisible); i++) {
        int rowY = listY + 2 + (i - scrollOffset) * ENTRY_HEIGHT;
        if (mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT) {
          selectedAccount = i;
          return true;
        }
      }

      if (clickButton(0, barY + 3, mouseX, mouseY)) {
        mode = Mode.OFFLINE;
        inputBuffer.setLength(0);
        setStatus(null, 0);
        return true;
      }
      if (clickButton(1, barY + 3, mouseX, mouseY)) {
        deleteSelected();
        return true;
      }
      return true;
    }

    if (clickButton(2, barY + 3, mouseX, mouseY)) {
      confirmInput();
      return true;
    }
    if (clickButton(0, barY + 3, mouseX, mouseY)) {
      inputBuffer.setLength(0);
      mode = Mode.LIST;
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
        && mouseX <= bx + buttonWidth()
        && mouseY >= btnY
        && mouseY <= btnY + BUTTON_HEIGHT;
  }

  @Override
  protected boolean consumeScroll(double mouseX, double mouseY, double scrollDelta) {
    if (mode != Mode.LIST) {
      return false;
    }
    int listHeight = height - TITLE_HEIGHT - 16 - 30;
    int maxVisible = Math.max(1, listHeight / ENTRY_HEIGHT);
    int maxScroll = Math.max(0, accounts().size() - maxVisible);
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

    if (mode == Mode.OFFLINE) {
      if (key == InputConstants.KEY_RETURN) {
        confirmInput();
        return true;
      }
      if (key == InputConstants.KEY_BACKSPACE) {
        if (inputBuffer.length() > 0) {
          inputBuffer.deleteCharAt(inputBuffer.length() - 1);
        } else {
          mode = Mode.LIST;
        }
        return true;
      }
      if (key == InputConstants.KEY_ESCAPE) {
        inputBuffer.setLength(0);
        mode = Mode.LIST;
        return true;
      }
      int codepoint = event.key();
      if (codepoint >= 32 && codepoint < 127 && inputBuffer.length() < 16) {
        inputBuffer.append((char) codepoint);
        return true;
      }
      return false;
    }

    List<Account> accounts = accounts();
    if (key == InputConstants.KEY_UP && selectedAccount > 0) {
      selectedAccount--;
      return true;
    }
    if (key == InputConstants.KEY_DOWN && selectedAccount < accounts.size() - 1) {
      selectedAccount++;
      return true;
    }
    if (key == InputConstants.KEY_DELETE) {
      deleteSelected();
      return true;
    }
    return false;
  }

  // Actions

  private List<Account> accounts() {
    return new ArrayList<>(Exeter.getInstance().getAccountManager().getRegistry());
  }

  private void confirmInput() {
    String username = inputBuffer.toString().trim();
    if (username.isEmpty()) {
      return;
    }
    if (username.length() > 16 || !username.matches("[A-Za-z0-9_]+")) {
      setStatus("§cInvalid username (A-Z, 0-9, _, max 16)", 5000);
      return;
    }
    registerAccount(username);
    inputBuffer.setLength(0);
    mode = Mode.LIST;
    setStatus("§aAdded " + username, 5000);
    DebugLogger.get().log(TAG, DebugLogger.Level.INFO, "Stored offline username " + username);
  }

  private static UUID offlineUuid(String username) {
    return UUID.nameUUIDFromBytes(
        ("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private void registerAccount(String username) {
    String uuid = offlineUuid(username).toString();
    for (Account existing : Exeter.getInstance().getAccountManager().getRegistry()) {
      if (existing.getUsername().equalsIgnoreCase(username)) {
        existing.setUsername(username);
        existing.setUuid(uuid);
        save();
        selectedAccount = Exeter.getInstance().getAccountManager().getRegistry().indexOf(existing);
        return;
      }
    }
    Exeter.getInstance().getAccountManager().register(new Account(username, uuid, 0L));
    save();
    selectedAccount = Exeter.getInstance().getAccountManager().getRegistry().size() - 1;
  }

  private void save() {
    Exeter.getInstance().getAccountManager().save();
  }

  private void deleteSelected() {
    List<Account> accounts = accounts();
    if (selectedAccount < 0 || selectedAccount >= accounts.size()) {
      return;
    }
    Exeter.getInstance().getAccountManager().unregister(accounts.get(selectedAccount));
    save();
    selectedAccount = -1;
  }
}
