package me.friendly.exeter.window.impl;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.proxy.ProxyEntry;
import me.friendly.exeter.proxy.ProxyManager;
import me.friendly.exeter.window.Window;
import me.friendly.exeter.window.WindowButtons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

/**
 * Proxy list window. Type {@code [name=][user:pass@]host:port} then Add. Type cycles the selected
 * entry between SOCKS5 and HTTP. Use marks the active proxy, On toggles proxying for game and auth
 * traffic.
 */
public class ProxyWindow extends Window {
  private static final int ENTRY_HEIGHT = 14;
  private static final int BUTTON_HEIGHT = 12;
  private static final int INPUT_HEIGHT = 14;
  private static final int BOTTOM_BAR_HEIGHT = 32;

  private final StringBuilder inputBuffer = new StringBuilder();
  private int selectedIndex = -1;
  private int scrollOffset = 0;
  private boolean cursorVisible = true;
  private long lastBlinkTime = System.currentTimeMillis();
  private ProxyEntry.Type pendingType = ProxyEntry.Type.SOCKS5;

  private volatile String status = null;
  private volatile long statusExpiry = 0;

  public ProxyWindow(int x, int y, int width, int height) {
    super("Proxies", x, y, width, height);
  }

  private ProxyManager proxies() {
    return Exeter.getInstance().getProxyManager();
  }

  @Override
  protected void renderContent(int mouseX, int mouseY, float partialTicks) {
    int contentY = y + TITLE_HEIGHT;
    int listY = contentY + 2;
    int barY = y + height - BOTTOM_BAR_HEIGHT;
    int listH = barY - listY;

    List<ProxyEntry> entries = new ArrayList<>(proxies().getRegistry());
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    scrollOffset = Math.min(scrollOffset, Math.max(0, entries.size() - maxVisible));

    int rowY = listY;
    for (int i = scrollOffset; i < Math.min(entries.size(), scrollOffset + maxVisible); i++) {
      ProxyEntry entry = entries.get(i);
      boolean hovered =
          mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT;
      if (hovered || i == selectedIndex) {
        RenderMethods.drawRect(x, rowY, x + width, rowY + ENTRY_HEIGHT, 0x20FFFFFF);
      }
      boolean isActive = entry.getName().equalsIgnoreCase(proxies().getActiveName());
      String display =
          entry.getName()
              + " §8"
              + entry.getType().name()
              + " "
              + entry.getHost()
              + ":"
              + entry.getPort();
      FontUtil.drawString(display, x + 5, rowY + 2, isActive ? 0xFF55FF55 : 0xFFEEEEEE);
      ProxyManager.PingResult ping = proxies().ping(entry);
      String pingText;
      int pingColor;
      if (ping.state == ProxyManager.PingState.OK) {
        pingText = "OK " + ping.ms + "ms";
        pingColor = 0xFF55FF55;
      } else if (ping.state == ProxyManager.PingState.FAIL) {
        pingText = "FAIL";
        pingColor = 0xFFFF5555;
      } else {
        pingText = "...";
        pingColor = 0xFF888888;
      }
      FontUtil.drawString(
          pingText, x + width - 5 - FontUtil.getStringWidth(pingText), rowY + 2, pingColor);
      rowY += ENTRY_HEIGHT;
    }

    if (entries.isEmpty()) {
      FontUtil.drawString("No proxies added.", x + width / 2 - 40, listY + listH / 2, 0xFF888888);
    }

    drawStatus(listY + listH - 12);
    RenderMethods.drawRect(x, barY, x + width, y + height, 0x77111111);

    int inputY = barY + 2;
    RenderMethods.drawRect(x + 3, inputY, x + width - 3, inputY + INPUT_HEIGHT, 0xFF222222);
    if (inputBuffer.length() == 0 && !isFocused()) {
      FontUtil.drawString("[name=][user:pass@]host:port...", x + 6, inputY + 3, 0xFF666666);
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

    int btnY = barY + INPUT_HEIGHT + 4;
    drawButton(0, btnY, mouseX, mouseY, "Add");
    drawButton(1, btnY, mouseX, mouseY, "Remove");
    drawButton(2, btnY, mouseX, mouseY, pendingType == ProxyEntry.Type.SOCKS5 ? "Socks" : "Http");
    drawButton(3, btnY, mouseX, mouseY, "Use");
    drawButton(4, btnY, mouseX, mouseY, proxies().isEnabled() ? "Disable" : "Enable");
  }

  private void drawStatus(int statusY) {
    String current = status();
    if (current != null) {
      FontUtil.drawString(
          current, x + width / 2 - FontUtil.getStringWidth(current) / 2, statusY, 0xFFFFFFFF);
    }
  }

  private String status() {
    if (status != null && System.currentTimeMillis() < statusExpiry) {
      return status;
    }
    status = null;
    ProxyEntry active = proxies().getActive();
    if (active == null) {
      return proxies().isEnabled() ? "Enabled, no proxy selected" : "Proxies disabled";
    }
    return active.getName() + " (" + active.getType().name() + ")";
  }

  private void setStatus(String message) {
    status = message;
    statusExpiry = System.currentTimeMillis() + 2500;
  }

  private final WindowButtons buttons = new WindowButtons(this, 5, BUTTON_HEIGHT);

  private int buttonWidth(int id) {
    return buttons.buttonWidth(id);
  }

  private int buttonX(int id) {
    return buttons.buttonX(id);
  }

  private void drawButton(int id, int btnY, int mouseX, int mouseY, String label) {
    buttons.drawButton(id, btnY, mouseX, mouseY, label);
  }

  private boolean clickButton(int id, int btnY, int mouseX, int mouseY) {
    return buttons.clickButton(id, btnY, mouseX, mouseY);
  }

  private ProxyEntry selected() {
    List<ProxyEntry> entries = proxies().getRegistry();
    if (selectedIndex < 0 || selectedIndex >= entries.size()) {
      return null;
    }
    return entries.get(selectedIndex);
  }

  private void addFromInput() {
    String text = inputBuffer.toString().trim();
    if (text.isEmpty()) {
      return;
    }
    ProxyEntry entry = ProxyEntry.parse(text, pendingType);
    if (entry == null) {
      setStatus("Bad format, use host:port");
      return;
    }
    proxies().getRegistry().add(entry);
    proxies().saveProxies();
    inputBuffer.setLength(0);
    setStatus("Added " + entry.getName());
  }

  private void removeSelected() {
    ProxyEntry entry = selected();
    if (entry == null) {
      return;
    }
    proxies().getRegistry().remove(entry);
    if (proxies().getActiveName().equalsIgnoreCase(entry.getName())) {
      proxies().setActive("");
    }
    proxies().saveProxies();
    selectedIndex = -1;
  }

  @Override
  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    if (button != InputConstants.MOUSE_BUTTON_LEFT) {
      return false;
    }
    int contentY = y + TITLE_HEIGHT;
    int listY = contentY + 2;
    int barY = y + height - BOTTOM_BAR_HEIGHT;
    int listH = barY - listY;

    List<ProxyEntry> entries = new ArrayList<>(proxies().getRegistry());
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    for (int i = scrollOffset; i < Math.min(entries.size(), scrollOffset + maxVisible); i++) {
      int rowY = listY + (i - scrollOffset) * ENTRY_HEIGHT;
      if (mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT) {
        selectedIndex = i;
        ProxyEntry entry = entries.get(i);
        proxies().setActive(entry.getName());
        proxies().saveProxies();
        return true;
      }
    }

    int btnY = barY + INPUT_HEIGHT + 4;
    if (clickButton(0, btnY, mouseX, mouseY)) {
      addFromInput();
      return true;
    }
    if (clickButton(1, btnY, mouseX, mouseY)) {
      removeSelected();
      return true;
    }
    if (clickButton(2, btnY, mouseX, mouseY)) {
      ProxyEntry entry = selected();
      if (entry != null) {
        ProxyEntry.Type next =
            entry.getType() == ProxyEntry.Type.SOCKS5
                ? ProxyEntry.Type.HTTP
                : ProxyEntry.Type.SOCKS5;
        int index = proxies().getRegistry().indexOf(entry);
        proxies()
            .getRegistry()
            .set(
                index,
                new ProxyEntry(
                    entry.getName(),
                    entry.getHost(),
                    entry.getPort(),
                    next,
                    entry.getUsername(),
                    entry.getPassword()));
        proxies().saveProxies();
      } else {
        pendingType =
            pendingType == ProxyEntry.Type.SOCKS5 ? ProxyEntry.Type.HTTP : ProxyEntry.Type.SOCKS5;
      }
      return true;
    }
    if (clickButton(3, btnY, mouseX, mouseY)) {
      ProxyEntry entry = selected();
      if (entry != null) {
        proxies().setActive(entry.getName());
        proxies().saveProxies();
        setStatus("Using " + entry.getName());
      }
      return true;
    }
    if (clickButton(4, btnY, mouseX, mouseY)) {
      proxies().setEnabled(!proxies().isEnabled());
      proxies().saveProxies();
      setStatus(proxies().isEnabled() ? "Proxies on" : "Proxies off");
      return true;
    }
    return false;
  }

  @Override
  protected boolean consumeScroll(double mouseX, double mouseY, double scrollDelta) {
    if (scrollDelta > 0) {
      scrollOffset = Math.max(0, scrollOffset - 1);
    } else if (scrollDelta < 0) {
      scrollOffset++;
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
    if (key == InputConstants.KEY_SPACE) {
      return true;
    }
    if (appendProxyChar(inputBuffer, event)) {
      return true;
    }
    return appendKeyChar(inputBuffer, event, 64);
  }

  private static boolean appendProxyChar(StringBuilder buffer, KeyEvent event) {
    if (buffer.length() >= 64) {
      return false;
    }
    boolean shift = Minecraft.getInstance().hasShiftDown();
    int code = event.key();
    if (code == InputConstants.KEY_PERIOD) {
      buffer.append('.');
      return true;
    }
    if (code == InputConstants.KEY_SLASH) {
      buffer.append('/');
      return true;
    }
    if (code == InputConstants.KEY_EQUALS) {
      buffer.append(shift ? '+' : '=');
      return true;
    }
    if (code == InputConstants.KEY_SEMICOLON) {
      buffer.append(shift ? ':' : ';');
      return true;
    }
    if (code == InputConstants.KEY_2 && shift) {
      buffer.append('@');
      return true;
    }
    return false;
  }
}
