package me.friendly.exeter.window.impl;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.toggle.render.clickgui.ColorPickerPopup;
import me.friendly.exeter.waypoint.Waypoint;
import me.friendly.exeter.waypoint.WaypointManager;
import me.friendly.exeter.window.Window;
import me.friendly.exeter.window.WindowButtons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

/**
 * Waypoint list window, Friends-style: click to select, Delete to remove, Enter or Add to create
 * from {@code name [x y z]} (bare names pin the player's current position).
 */
public class WaypointWindow extends Window {
  private static final int ENTRY_HEIGHT = 14;
  private static final int BUTTON_HEIGHT = 12;
  private static final int INPUT_HEIGHT = 14;
  private static final int BOTTOM_BAR_HEIGHT = 32;

  private final StringBuilder inputBuffer = new StringBuilder();
  private int selectedIndex = -1;
  private int scrollOffset = 0;
  private boolean cursorVisible = true;
  private long lastBlinkTime = System.currentTimeMillis();

  private volatile String status = null;
  private volatile long statusExpiry = 0;
  private ColorPickerPopup colorPicker = null;

  public WaypointWindow(int x, int y, int width, int height) {
    super("Waypoints", x, y, width, height);
  }

  private WaypointManager manager() {
    return Exeter.getInstance().getWaypointManager();
  }

  @Override
  protected void renderContent(int mouseX, int mouseY, float partialTicks) {
    int contentY = y + TITLE_HEIGHT;
    int listY = contentY + 2;
    int barY = y + height - BOTTOM_BAR_HEIGHT;
    int listH = barY - listY;

    List<Waypoint> waypoints = new ArrayList<>(manager().getRegistry());
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    scrollOffset = Math.min(scrollOffset, Math.max(0, waypoints.size() - maxVisible));

    int rowY = listY;
    for (int i = scrollOffset; i < Math.min(waypoints.size(), scrollOffset + maxVisible); i++) {
      Waypoint waypoint = waypoints.get(i);
      boolean hovered =
          mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT;
      if (hovered || i == selectedIndex) {
        RenderMethods.drawRect(x, rowY, x + width, rowY + ENTRY_HEIGHT, 0x20FFFFFF);
      }
      String marker = waypoint.isEnabled() ? "§a[Enabled] " : "§8[Disabled] ";
      String display = marker + waypoint.getName() + " §8(" + waypoint.coordsShort() + "§8)";
      int swatch = 0xFF000000 | waypoint.getColor();
      RenderMethods.drawRect(x + 5, rowY + 3, x + 11, rowY + 9, swatch);
      FontUtil.drawString(display, x + 14, rowY + 2, 0xFFEEEEEE);
      rowY += ENTRY_HEIGHT;
    }

    if (waypoints.isEmpty()) {
      FontUtil.drawString("No waypoints added.", x + width / 2 - 50, listY + listH / 2, 0xFF888888);
    }

    drawStatus(listY + listH - 12);

    RenderMethods.drawRect(x, barY, x + width, y + height, 0x77111111);

    int inputY = barY + 2;
    RenderMethods.drawRect(x + 3, inputY, x + width - 3, inputY + INPUT_HEIGHT, 0xFF222222);
    if (inputBuffer.length() == 0 && !isFocused()) {
      FontUtil.drawString("name [x y z]...", x + 6, inputY + 3, 0xFF666666);
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
    drawButton(2, btnY, mouseX, mouseY, isSelectedEnabled() ? "Disable" : "Enable");
    drawButton(3, btnY, mouseX, mouseY, "Add Here");
    drawButton(4, btnY, mouseX, mouseY, "Color");
  }

  @Override
  public void render(int mouseX, int mouseY, float partialTicks) {
    super.render(mouseX, mouseY, partialTicks);
    // Above everything else in this window (including the focus outline).
    if (colorPicker != null) {
      var window = Minecraft.getInstance().getWindow();
      colorPicker.render(
          mouseX, mouseY, partialTicks, window.getGuiScaledWidth(), window.getGuiScaledHeight());
    }
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

  private final WindowButtons buttons =
      new WindowButtons(
          this,
          5,
          BUTTON_HEIGHT,
          id ->
              (id != 1 && id != 4)
                  || (selectedIndex >= 0 && selectedIndex < manager().getRegistry().size()));

  private void drawButton(int id, int btnY, int mouseX, int mouseY, String label) {
    buttons.drawButton(id, btnY, mouseX, mouseY, label);
  }

  private boolean clickButton(int id, int btnY, int mouseX, int mouseY) {
    return buttons.clickButton(id, btnY, mouseX, mouseY);
  }

  @Override
  public boolean isModal() {
    return colorPicker != null;
  }

  @Override
  public boolean mouseClicked(int mouseX, int mouseY, int button) {
    // The picker is screen-centered, usually outside our bounds, so it needs clicks first.
    if (!isHidden() && colorPicker != null) {
      if (!colorPicker.mouseClicked(mouseX, mouseY, button)) {
        closeColorPicker();
      }
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    if (button != InputConstants.MOUSE_BUTTON_LEFT) {
      return false;
    }
    if (colorPicker != null) {
      // Modal: clicks outside the popup dismiss it instead of touching the list.
      if (!colorPicker.mouseClicked(mouseX, mouseY, button)) {
        closeColorPicker();
      }
      return true;
    }
    int contentY = y + TITLE_HEIGHT;
    int listY = contentY + 2;
    int barY = y + height - BOTTOM_BAR_HEIGHT;
    int listH = barY - listY;

    List<Waypoint> waypoints = new ArrayList<>(manager().getRegistry());
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    for (int i = scrollOffset; i < Math.min(waypoints.size(), scrollOffset + maxVisible); i++) {
      int rowY = listY + (i - scrollOffset) * ENTRY_HEIGHT;
      if (mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT) {
        selectedIndex = i;
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
      toggleSelected();
      return true;
    }
    if (clickButton(3, btnY, mouseX, mouseY)) {
      addHere();
      return true;
    }
    if (clickButton(4, btnY, mouseX, mouseY)) {
      openColorPicker();
      return true;
    }
    return true;
  }

  @Override
  protected boolean consumeDragged(int mouseX, int mouseY) {
    if (colorPicker != null) {
      return colorPicker.mouseDragged(mouseX, mouseY);
    }
    return false;
  }

  @Override
  public void mouseReleased(int button) {
    super.mouseReleased(button);
    if (colorPicker != null) {
      colorPicker.mouseReleased(0, 0, button);
    }
  }

  @Override
  protected boolean consumeScroll(double mouseX, double mouseY, double scrollDelta) {
    List<Waypoint> waypoints = manager().getRegistry();
    int listH = (y + height - BOTTOM_BAR_HEIGHT) - (y + TITLE_HEIGHT + 2);
    int maxVisible = Math.max(1, listH / ENTRY_HEIGHT);
    int maxScroll = Math.max(0, waypoints.size() - maxVisible);
    if (scrollDelta < 0) {
      scrollOffset = Math.min(maxScroll, scrollOffset + 1);
    } else {
      scrollOffset = Math.max(0, scrollOffset - 1);
    }
    return true;
  }

  @Override
  protected boolean consumeKeyPress(KeyEvent event) {
    if (colorPicker != null) {
      return colorPicker.keyPressed(event.key(), event.keycode(), event.modifiers());
    }
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

    return appendKeyChar(inputBuffer, event, 64);
  }

  private void addFromInput() {
    String text = inputBuffer.toString().trim();
    if (text.isEmpty()) {
      return;
    }
    String[] parts = text.split("\\s+");
    String result = manager().addWaypoint(parts);
    if (result == null) {
      inputBuffer.setLength(0);
      selectedIndex = manager().getRegistry().size() - 1;
      setStatus("§aAdded waypoint (" + parts[0] + ")", 3000);
    } else {
      setStatus("§c" + result, 3000);
    }
  }

  private void addHere() {
    Waypoint pinned = manager().addHere();
    if (pinned == null) {
      setStatus("§cNo position: join a world first", 3000);
      return;
    }
    selectedIndex = manager().getRegistry().indexOf(pinned);
    setStatus("§aAdded waypoint (" + pinned.getName() + ")", 3000);
  }

  private void openColorPicker() {
    List<Waypoint> waypoints = manager().getRegistry();
    if (selectedIndex < 0 || selectedIndex >= waypoints.size()) {
      setStatus("§cSelect a waypoint first", 3000);
      return;
    }
    Waypoint waypoint = waypoints.get(selectedIndex);
    colorPicker =
        new ColorPickerPopup(
            waypoint::getColor, picked -> waypoint.setColor(picked), this::closeColorPicker);
  }

  private void closeColorPicker() {
    colorPicker = null;
    manager().saveWaypoints();
  }

  private void removeSelected() {
    List<Waypoint> waypoints = manager().getRegistry();
    if (selectedIndex < 0 || selectedIndex >= waypoints.size()) {
      return;
    }
    Waypoint removed = waypoints.get(selectedIndex);
    manager().remove(removed.getName());
    setStatus("§7Removed (" + removed.getName() + ")", 3000);
    selectedIndex = -1;
  }

  private boolean isSelectedEnabled() {
    List<Waypoint> waypoints = manager().getRegistry();
    return selectedIndex >= 0
        && selectedIndex < waypoints.size()
        && waypoints.get(selectedIndex).isEnabled();
  }

  private void toggleSelected() {
    List<Waypoint> waypoints = manager().getRegistry();
    Waypoint current =
        selectedIndex >= 0 && selectedIndex < waypoints.size()
            ? waypoints.get(selectedIndex)
            : null;
    if (current == null) {
      return;
    }
    current.setEnabled(!current.isEnabled());
    manager().saveWaypoints();
    setStatus((current.isEnabled() ? "§aShowing (" : "§8Hidden (") + current.getName() + ")", 3000);
  }
}
