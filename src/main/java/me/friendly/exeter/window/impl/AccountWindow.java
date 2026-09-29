package me.friendly.exeter.window.impl;

import com.mojang.blaze3d.platform.InputConstants;
import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.account.Account;
import me.friendly.exeter.account.auth.MicrosoftAuth;
import me.friendly.exeter.account.auth.SessionManager;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.window.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

/**
 * In-game alt/account manager.
 *
 * <p>Replaces the non-functional legacy {@code gui.screens.accountmanager} package. The Microsoft
 * login, token, and session flows are ported from OpenMyau's {@code GuiAccountManager}, {@code
 * GuiMicrosoftAuth}, {@code GuiAddToken}, and {@code GuiSessionLogin} (derived from
 * https://github.com/ksyzov/AccountManager, originally LGPL, modified version GPL v3) and reworked
 * onto Exeter's {@link Window} system.
 */
public class AccountWindow extends Window {
  private static final String TAG = "AccountManager";
  private static final int ENTRY_HEIGHT = 14;
  private static final int BUTTON_HEIGHT = 12;
  private static final int BUTTON_COUNT = 6;

  private enum Mode {
    LIST,
    BROWSER,
    TOKEN,
    SESSION,
    OFFLINE
  }

  private volatile Mode mode = Mode.LIST;
  private String browserState = "";
  private final StringBuilder inputBuffer = new StringBuilder();

  private volatile int selectedAccount = -1;
  private int scrollOffset = 0;
  private long lastClickTime = 0;
  private int lastClickIndex = -1;

  private volatile String status = null;
  private volatile long statusExpiry = 0;

  private ExecutorService executor = null;
  private CompletableFuture<?> task = null;

  public AccountWindow(int x, int y, int width, int height) {
    super("Accounts", x, y, width, height);
  }

  // Rendering

  @Override
  protected void renderContent(int mouseX, int mouseY, float partialTicks) {
    int contentY = y + TITLE_HEIGHT;
    switch (mode) {
      case LIST -> renderList(mouseX, mouseY, contentY);
      case BROWSER -> renderBrowser(mouseX, mouseY, contentY);
      case TOKEN -> renderInput(mouseX, mouseY, contentY, "Paste Microsoft refresh token");
      case SESSION -> renderInput(mouseX, mouseY, contentY, "Paste name:uuid:token or MC token");
      case OFFLINE -> renderInput(mouseX, mouseY, contentY, "Enter offline username");
    }
  }

  private void renderList(int mouseX, int mouseY, int contentY) {
    List<Account> accounts = accounts();
    String current = SessionManager.get().getName();
    FontUtil.drawString(
        "§7Current: §3" + current + " §8(" + accounts.size() + ")",
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
        name = "§r" + name;
      }
      FontUtil.drawString(name + " " + account.getTypeTag(), x + 5, rowY + 2, 0xFFFFFFFF);

      String ban = banText(account);
      FontUtil.drawString(ban, x + width - 5 - FontUtil.getStringWidth(ban), rowY + 2, 0xFFFFFFFF);

      rowY += ENTRY_HEIGHT;
    }

    if (accounts.isEmpty()) {
      FontUtil.drawString(
          "No accounts. Use MS Auth,", x + width / 2 - 80, listY + listHeight / 2 - 6, 0xFF888888);
      FontUtil.drawString(
          "Token, Session or Offline.", x + width / 2 - 80, listY + listHeight / 2 + 6, 0xFF888888);
    }

    int barY = contentY + height - TITLE_HEIGHT - 28;
    RenderMethods.drawRect(x, barY, x + width, y + height, 0x77111111);
    drawButton(0, barY + 3, mouseX, mouseY, "Login");
    drawButton(1, barY + 3, mouseX, mouseY, "MS Auth");
    drawButton(2, barY + 3, mouseX, mouseY, "Token");
    drawButton(3, barY + 3, mouseX, mouseY, "Session");
    drawButton(4, barY + 3, mouseX, mouseY, "Offline");
    drawButton(5, barY + 3, mouseX, mouseY, "Delete");
    drawStatus(barY - 14);
  }

  private void renderBrowser(int mouseX, int mouseY, int contentY) {
    FontUtil.drawString("Microsoft Authentication", x + width / 2 - 70, contentY + 30, 0xFFAAAAAA);
    FontUtil.drawString(
        "Login link opened + copied.", x + width / 2 - 75, contentY + 48, 0xFFFFFFFF);
    String current = status();
    if (current != null) {
      FontUtil.drawString(
          current, x + width / 2 - FontUtil.getStringWidth(current) / 2, contentY + 62, 0xFFFFFFFF);
    }
    int barY = contentY + height - TITLE_HEIGHT - 28;
    RenderMethods.drawRect(x, barY, x + width, y + height, 0x77111111);
    drawButton(6, barY + 3, mouseX, mouseY, "Open Link");
    drawButton(7, barY + 3, mouseX, mouseY, "Cancel");
    drawButton(9, barY + 3, mouseX, mouseY, "Done");
  }

  private void renderInput(int mouseX, int mouseY, int contentY, String title) {
    FontUtil.drawString(
        title, x + width / 2 - FontUtil.getStringWidth(title) / 2, contentY + 30, 0xFFAAAAAA);
    int fieldX = x + 10;
    int fieldY = contentY + 48;
    RenderMethods.drawRect(fieldX, fieldY, x + width - 10, fieldY + 14, 0xFF222222);
    String text = inputBuffer.toString();
    int maxChars = 60;
    String shown =
        text.length() > maxChars ? "..." + text.substring(text.length() - maxChars) : text;
    FontUtil.drawString(shown + "_", fieldX + 3, fieldY + 3, 0xFFEEEEEE);
    FontUtil.drawString(
        "Enter = confirm, Esc clears, Ctrl+V = paste",
        x + width / 2 - 105,
        fieldY + 20,
        0xFF888888);
    String current = status();
    if (current != null) {
      FontUtil.drawString(
          current, x + width / 2 - FontUtil.getStringWidth(current) / 2, fieldY + 34, 0xFFFFFFFF);
    }
    int barY = contentY + height - TITLE_HEIGHT - 28;
    RenderMethods.drawRect(x, barY, x + width, y + height, 0x77111111);
    drawButton(8, barY + 3, mouseX, mouseY, "Confirm");
    drawButton(7, barY + 3, mouseX, mouseY, "Cancel");
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
      case 0, 5 ->
          mode == Mode.LIST
              && selectedAccount >= 0
              && selectedAccount < accounts().size()
              && (task == null || task.isDone());
      case 1, 2, 3, 4 -> mode == Mode.LIST && (task == null || task.isDone());
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
          long now = System.currentTimeMillis();
          if (lastClickIndex == i && now - lastClickTime < 300) {
            selectedAccount = i;
            loginSelected();
          } else {
            selectedAccount = i;
            lastClickIndex = i;
            lastClickTime = now;
          }
          return true;
        }
      }

      if (clickButton(0, barY + 3, mouseX, mouseY)) {
        loginSelected();
        return true;
      }
      if (clickButton(1, barY + 3, mouseX, mouseY)) {
        startBrowserAuth();
        return true;
      }
      if (clickButton(2, barY + 3, mouseX, mouseY)) {
        enterInputMode(Mode.TOKEN);
        return true;
      }
      if (clickButton(3, barY + 3, mouseX, mouseY)) {
        enterInputMode(Mode.SESSION);
        return true;
      }
      if (clickButton(4, barY + 3, mouseX, mouseY)) {
        enterInputMode(Mode.OFFLINE);
        return true;
      }
      if (clickButton(5, barY + 3, mouseX, mouseY)) {
        deleteSelected();
        return true;
      }
      return true;
    }

    if (mode == Mode.BROWSER) {
      if (clickButton(6, barY + 3, mouseX, mouseY)) {
        openBrowserLink();
        return true;
      }
      if (clickButton(7, barY + 3, mouseX, mouseY)) {
        cancelTask();
        mode = Mode.LIST;
        return true;
      }
      if (clickButton(9, barY + 3, mouseX, mouseY)) {
        mode = Mode.LIST;
        return true;
      }
      return true;
    }

    if (clickButton(8, barY + 3, mouseX, mouseY)) {
      confirmInput();
      return true;
    }
    if (clickButton(7, barY + 3, mouseX, mouseY)) {
      cancelTask();
      inputBuffer.setLength(0);
      mode = Mode.LIST;
      return true;
    }
    return true;
  }

  private void enterInputMode(Mode next) {
    mode = next;
    inputBuffer.setLength(0);
    setStatus(null, 0);
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

    if (mode == Mode.TOKEN || mode == Mode.SESSION || mode == Mode.OFFLINE) {
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
      if (key == InputConstants.KEY_V && Minecraft.getInstance().hasControlDown()) {
        String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clip != null) {
          inputBuffer.append(clip.trim());
        }
        return true;
      }
      int codepoint = event.key();
      if (codepoint >= 32 && codepoint < 127 && inputBuffer.length() < 1024) {
        inputBuffer.append((char) codepoint);
        return true;
      }
      return false;
    }

    if (mode != Mode.LIST) {
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
    if (key == InputConstants.KEY_RETURN) {
      loginSelected();
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

  private void save() {
    Exeter.getInstance().getAccountManager().save();
  }

  private ExecutorService executor() {
    if (executor == null || executor.isShutdown()) {
      executor =
          Executors.newSingleThreadExecutor(
              runnable -> {
                Thread thread = new Thread(runnable, "Exeter-AccountAuth");
                thread.setDaemon(true);
                return thread;
              });
    }
    return executor;
  }

  private void cancelTask() {
    if (task != null && !task.isDone()) {
      task.cancel(true);
    }
    if (executor != null) {
      executor.shutdownNow();
      executor = null;
    }
  }

  private String displayName(Account account) {
    String name = account.getUsername();
    return name == null || name.isBlank() ? "???" : name;
  }

  private boolean taskIdle() {
    return task == null || task.isDone();
  }

  private void loginSelected() {
    if (selectedAccount < 0 || selectedAccount >= accounts().size() || !taskIdle()) {
      return;
    }
    Account account = accounts().get(selectedAccount);
    switch (account.getType()) {
      case OFFLINE -> {
        SessionManager.setOffline(account.getUsername());
        setStatus("§aOffline login! (" + account.getUsername() + ")", 5000);
        DebugLogger.get()
            .log(TAG, DebugLogger.Level.INFO, "Offline login " + account.getUsername());
      }
      case SESSION -> loginSessionAccount(account);
      case MICROSOFT -> loginMicrosoftAccount(account);
    }
  }

  /** Microsoft login: fast path with the stored token, full refresh chain on failure. */
  private void loginMicrosoftAccount(Account account) {
    String username = displayName(account);
    setStatus("§7Fetching profile... (" + username + ")", 0);
    DebugLogger.get().logFile(TAG, "Logging in " + username);

    MicrosoftAuth.CLIENT_ID =
        account.getClientId().isBlank() ? MicrosoftAuth.CLIENT_ID : account.getClientId();
    MicrosoftAuth.SCOPE = account.getScope().isBlank() ? MicrosoftAuth.SCOPE : account.getScope();
    ExecutorService exec = executor();
    AtomicReference<String> newRefresh = new AtomicReference<>("");
    AtomicReference<String> newAccess = new AtomicReference<>("");

    task =
        MicrosoftAuth.fetchProfile(account.getAccessToken(), exec)
            .thenAccept(
                profile -> {
                  account.setUsername(profile.username());
                  account.setUuid(profile.uuid().toString());
                  save();
                  SessionManager.set(profile, account.getAccessToken());
                  setStatus("§aLogin successful! (" + account.getUsername() + ")", 5000);
                  DebugLogger.get()
                      .log(TAG, DebugLogger.Level.INFO, "Logged in " + account.getUsername());
                })
            .exceptionallyCompose(
                error -> {
                  setStatus("§7Refreshing tokens... (" + username + ")", 0);
                  return refreshChain(account, newRefresh, newAccess, exec);
                });
  }

  /** Session-token login: validates the stored token, no refresh path exists. */
  private void loginSessionAccount(Account account) {
    String username = displayName(account);
    setStatus("§7Validating token... (" + username + ")", 0);
    ExecutorService exec = executor();
    task =
        MicrosoftAuth.fetchProfile(account.getAccessToken(), exec)
            .thenAccept(
                profile -> {
                  account.setUsername(profile.username());
                  account.setUuid(profile.uuid().toString());
                  save();
                  SessionManager.set(profile, account.getAccessToken());
                  setStatus("§aLogin successful! (" + account.getUsername() + ")", 5000);
                  DebugLogger.get()
                      .log(TAG, DebugLogger.Level.INFO, "Logged in " + account.getUsername());
                })
            .exceptionally(
                error -> {
                  setStatus("§cToken expired, re-add it (" + username + ")", 8000);
                  DebugLogger.get().logFile(TAG, "Session login failed for " + username);
                  return null;
                });
  }

  /** Full Microsoft -> Xbox -> Minecraft token refresh, shared by login and token import. */
  private CompletableFuture<Void> refreshChain(
      Account account,
      AtomicReference<String> newRefresh,
      AtomicReference<String> newAccess,
      ExecutorService exec) {
    String username = displayName(account);
    return MicrosoftAuth.refreshMSAccessTokens(account.getRefreshToken(), exec)
        .thenCompose(
            msTokens -> {
              newRefresh.set(msTokens.get("refresh_token"));
              setStatus("§7Acquiring Xbox token... (" + username + ")", 0);
              return MicrosoftAuth.acquireXboxAccessToken(msTokens.get("access_token"), exec);
            })
        .thenCompose(
            xboxToken -> {
              setStatus("§7Acquiring Xbox XSTS token... (" + username + ")", 0);
              return MicrosoftAuth.acquireXboxXstsToken(xboxToken, exec);
            })
        .thenCompose(
            xsts -> {
              setStatus("§7Acquiring Minecraft token... (" + username + ")", 0);
              return MicrosoftAuth.acquireMCAccessToken(xsts.get("Token"), xsts.get("uhs"), exec);
            })
        .thenCompose(
            mcToken -> {
              newAccess.set(mcToken);
              setStatus("§7Fetching profile... (" + username + ")", 0);
              return MicrosoftAuth.fetchProfile(mcToken, exec);
            })
        .thenAccept(
            profile -> {
              account.setRefreshToken(newRefresh.get());
              account.setAccessToken(newAccess.get());
              account.setUsername(profile.username());
              account.setUuid(profile.uuid().toString());
              save();
              SessionManager.set(profile, newAccess.get());
              setStatus("§aLogin successful! (" + account.getUsername() + ")", 5000);
              DebugLogger.get()
                  .log(TAG, DebugLogger.Level.INFO, "Logged in " + account.getUsername());
            })
        .exceptionally(
            error -> {
              String message =
                  error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
              setStatus("§c" + message + " (" + username + ")", 8000);
              DebugLogger.get().logFile(TAG, "Login failed for " + username + ": " + message);
              return null;
            });
  }

  private void startBrowserAuth() {
    cancelTask();
    MicrosoftAuth.CLIENT_ID = "42a60a84-599d-44b2-a7c6-b00cdef1d6a2";
    MicrosoftAuth.SCOPE = "XboxLive.signin XboxLive.offline_access";
    browserState = UUID.randomUUID().toString().substring(0, 8);
    mode = Mode.BROWSER;
    openBrowserLink();
    setStatus("§7Waiting for browser login...", 0);

    ExecutorService exec = executor();
    String state = browserState;
    AtomicReference<String> newRefresh = new AtomicReference<>("");
    AtomicReference<String> newAccess = new AtomicReference<>("");
    task =
        MicrosoftAuth.acquireMSAuthCode(state, exec)
            .thenCompose(code -> MicrosoftAuth.acquireMSAccessTokens(code, exec))
            .thenCompose(
                msTokens -> {
                  newRefresh.set(msTokens.get("refresh_token"));
                  setStatus("§7Acquiring Xbox token...", 0);
                  return MicrosoftAuth.acquireXboxAccessToken(msTokens.get("access_token"), exec);
                })
            .thenCompose(
                xboxToken -> {
                  setStatus("§7Acquiring Xbox XSTS token...", 0);
                  return MicrosoftAuth.acquireXboxXstsToken(xboxToken, exec);
                })
            .thenCompose(
                xsts -> {
                  setStatus("§7Acquiring Minecraft token...", 0);
                  return MicrosoftAuth.acquireMCAccessToken(
                      xsts.get("Token"), xsts.get("uhs"), exec);
                })
            .thenCompose(
                mcToken -> {
                  newAccess.set(mcToken);
                  setStatus("§7Fetching profile...", 0);
                  return MicrosoftAuth.fetchProfile(mcToken, exec);
                })
            .thenAccept(
                profile -> {
                  registerAccount(
                      newRefresh.get(),
                      newAccess.get(),
                      profile,
                      MicrosoftAuth.CLIENT_ID,
                      MicrosoftAuth.SCOPE,
                      Account.Type.MICROSOFT);
                  SessionManager.set(profile, newAccess.get());
                  setStatus("§aLogin successful! (" + profile.username() + ")", 5000);
                })
            .exceptionally(
                error -> {
                  String message =
                      error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
                  setStatus("§c" + message, 8000);
                  DebugLogger.get().logFile(TAG, "Browser auth failed: " + message);
                  return null;
                });
  }

  private void openBrowserLink() {
    URI url = MicrosoftAuth.getMSAuthLink(browserState);
    if (url == null) {
      setStatus("§cFailed to build login link", 5000);
      return;
    }
    Minecraft.getInstance().keyboardHandler.setClipboard(url.toString());
    try {
      Desktop.getDesktop().browse(url);
    } catch (Exception e) {
      setStatus("§cOpen the link from your clipboard", 5000);
    }
  }

  private void confirmInput() {
    String text = inputBuffer.toString().trim();
    if (text.isEmpty()) {
      return;
    }
    switch (mode) {
      case SESSION -> loginViaSession(text);
      case TOKEN -> loginViaToken(text);
      case OFFLINE -> loginViaOffline(text);
      default -> {}
    }
  }

  /**
   * Direct session login, ported from upstream {@code GuiSessionLogin}. Successful logins are
   * stored as session-token accounts so the list shows what they are.
   */
  private void loginViaSession(String text) {
    if (!taskIdle()) {
      return;
    }
    if (text.contains(":")) {
      String[] parts = text.split(":");
      if (parts.length >= 3) {
        try {
          UUID uuid = parseUuid(parts[1]);
          SessionManager.set(parts[0], uuid, parts[2]);
          registerAccount("", parts[2], parts[0], uuid.toString(), Account.Type.SESSION);
          inputBuffer.setLength(0);
          mode = Mode.LIST;
          setStatus("§aSession set! (" + parts[0] + ")", 5000);
        } catch (Exception e) {
          setStatus("§cInvalid name:uuid:token format", 5000);
        }
        return;
      }
    }
    setStatus("§7Validating token...", 0);
    ExecutorService exec = executor();
    task =
        MicrosoftAuth.fetchProfile(text, exec)
            .thenAccept(
                profile -> {
                  SessionManager.set(profile, text);
                  registerAccount("", text, profile, "", "", Account.Type.SESSION);
                  inputBuffer.setLength(0);
                  mode = Mode.LIST;
                  setStatus("§aSession set! (" + profile.username() + ")", 5000);
                })
            .exceptionally(
                error -> {
                  setStatus("§cInvalid session token", 5000);
                  return null;
                });
  }

  /** Offline login: no tokens, deterministic offline UUID, stored as an offline account. */
  private void loginViaOffline(String username) {
    if (username.isBlank() || username.length() > 16 || !username.matches("[A-Za-z0-9_]+")) {
      setStatus("§cInvalid username (A-Z, 0-9, _, max 16)", 5000);
      return;
    }
    SessionManager.setOffline(username);
    registerAccount("", "", username, offlineUuid(username).toString(), Account.Type.OFFLINE);
    inputBuffer.setLength(0);
    mode = Mode.LIST;
    setStatus("§aOffline login! (" + username + ")", 5000);
    DebugLogger.get().log(TAG, DebugLogger.Level.INFO, "Offline login " + username);
  }

  private static UUID offlineUuid(String username) {
    return UUID.nameUUIDFromBytes(
        ("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /** Token import, ported from upstream {@code GuiAddToken}. */
  private void loginViaToken(String text) {
    if (!taskIdle()) {
      return;
    }
    cancelTask();
    MicrosoftAuth.CLIENT_ID = MicrosoftAuth.TOKEN_CLIENT_ID;
    MicrosoftAuth.SCOPE = MicrosoftAuth.TOKEN_SCOPE;
    setStatus("§7Validating token...", 0);
    ExecutorService exec = executor();
    AtomicReference<String> newRefresh = new AtomicReference<>("");
    AtomicReference<String> newAccess = new AtomicReference<>("");
    task =
        MicrosoftAuth.fetchProfile(text, exec)
            // Raw Minecraft token: valid ~24h, no refresh path, so it is a session account.
            .thenAccept(profile -> addTokenAccount(text, text, profile, Account.Type.SESSION))
            .exceptionallyCompose(
                error -> {
                  setStatus("§7Refreshing Microsoft tokens...", 0);
                  return MicrosoftAuth.refreshMSAccessTokens(text, exec)
                      .thenCompose(
                          msTokens -> {
                            newRefresh.set(msTokens.get("refresh_token"));
                            setStatus("§7Acquiring Xbox token...", 0);
                            return MicrosoftAuth.acquireXboxAccessToken(
                                msTokens.get("access_token"), exec);
                          })
                      .thenCompose(
                          xboxToken -> {
                            setStatus("§7Acquiring Xbox XSTS token...", 0);
                            return MicrosoftAuth.acquireXboxXstsToken(xboxToken, exec);
                          })
                      .thenCompose(
                          xsts -> {
                            setStatus("§7Acquiring Minecraft token...", 0);
                            return MicrosoftAuth.acquireMCAccessToken(
                                xsts.get("Token"), xsts.get("uhs"), exec);
                          })
                      .thenCompose(
                          mcToken -> {
                            newAccess.set(mcToken);
                            setStatus("§7Fetching profile...", 0);
                            return MicrosoftAuth.fetchProfile(mcToken, exec);
                          })
                      .thenAccept(
                          // Full exchange yields refreshable tokens: a Microsoft account.
                          profile ->
                              addTokenAccount(
                                  newRefresh.get(),
                                  newAccess.get(),
                                  profile,
                                  Account.Type.MICROSOFT));
                })
            .exceptionally(
                error -> {
                  String message =
                      error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
                  setStatus("§c" + message, 8000);
                  DebugLogger.get().logFile(TAG, "Token import failed: " + message);
                  return null;
                });
  }

  private void addTokenAccount(
      String refreshToken,
      String accessToken,
      MicrosoftAuth.MinecraftProfile profile,
      Account.Type type) {
    registerAccount(
        refreshToken, accessToken, profile, MicrosoftAuth.CLIENT_ID, MicrosoftAuth.SCOPE, type);
    SessionManager.set(profile, accessToken);
    inputBuffer.setLength(0);
    mode = Mode.LIST;
    setStatus("§aToken added! (" + profile.username() + ")", 5000);
  }

  private void registerAccount(
      String refreshToken,
      String accessToken,
      MicrosoftAuth.MinecraftProfile profile,
      String clientId,
      String scope,
      Account.Type type) {
    registerAccount(
        refreshToken,
        accessToken,
        profile.username(),
        profile.uuid().toString(),
        clientId,
        scope,
        type);
  }

  private void registerAccount(
      String refreshToken, String accessToken, String username, String uuid, Account.Type type) {
    registerAccount(refreshToken, accessToken, username, uuid, "", "", type);
  }

  private void registerAccount(
      String refreshToken,
      String accessToken,
      String username,
      String uuid,
      String clientId,
      String scope,
      Account.Type type) {
    for (Account existing : Exeter.getInstance().getAccountManager().getRegistry()) {
      if (existing.getUsername().equalsIgnoreCase(username) && existing.getType() == type) {
        existing.setRefreshToken(refreshToken);
        existing.setAccessToken(accessToken);
        existing.setUsername(username);
        existing.setUuid(uuid);
        existing.setClientId(clientId);
        existing.setScope(scope);
        save();
        selectedAccount = Exeter.getInstance().getAccountManager().getRegistry().indexOf(existing);
        DebugLogger.get().log(TAG, DebugLogger.Level.INFO, "Updated account " + username);
        return;
      }
    }
    Account account =
        new Account(refreshToken, accessToken, username, uuid, 0L, clientId, scope, type);
    Exeter.getInstance().getAccountManager().register(account);
    save();
    selectedAccount = Exeter.getInstance().getAccountManager().getRegistry().size() - 1;
    DebugLogger.get().log(TAG, DebugLogger.Level.INFO, "Added account " + username);
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

  private static UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (Exception e) {
      return MicrosoftAuth.parseUndashedUuid(value);
    }
  }
}
