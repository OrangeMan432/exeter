package me.friendly.exeter.window.impl;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.macro.Macro;
import me.friendly.exeter.macro.MacroManager;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.util.KeyNames;
import me.friendly.exeter.window.Window;
import me.friendly.exeter.window.WindowButtons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

/**
 * Macro manager window, tabbed: one tab per macro plus a create tab. Each macro tab shows its info
 * buttons, a list-tab row (commands / on-enable / on-disable) and the visible command list with its
 * own input. No shared input state, so creating the tenth macro works exactly like the first.
 */
public class MacroWindow extends Window {
  private static final int ENTRY_HEIGHT = 14;
  private static final int BUTTON_HEIGHT = 12;
  private static final int INPUT_HEIGHT = 14;
  private static final int TAB_HEIGHT = 13;

  private enum ListTab {
    COMMANDS,
    ON_ENABLE,
    ON_DISABLE
  }

  private enum Field {
    NAME,
    COMMAND
  }

  private record TabRect(String text, int x, int y, int w, int macroIndex, boolean plus) {}

  private final StringBuilder nameBuffer = new StringBuilder();
  private final StringBuilder commandBuffer = new StringBuilder();
  private Field focusedField = Field.NAME;
  private int selectedIndex = -1;
  private int selectedLine = -1;
  private int scrollOffset;
  private ListTab listTab = ListTab.COMMANDS;
  private boolean capturing;
  private boolean cursorVisible = true;
  private long lastBlinkTime = System.currentTimeMillis();

  private volatile String status;
  private volatile long statusExpiry;

  public MacroWindow(int x, int y, int width, int height) {
    super("Macros", x, y, width, height);
  }

  private MacroManager macros() {
    return Exeter.getInstance().getMacroManager();
  }

  private List<Macro> all() {
    return macros().getRegistry();
  }

  private Macro selected() {
    List<Macro> all = all();
    if (selectedIndex < 0 || selectedIndex >= all.size()) return null;
    return all.get(selectedIndex);
  }

  private List<String> visibleLines(Macro macro) {
    if (macro == null) return List.of();
    if (macro.getMode() == Macro.Mode.INSTANT) return macro.getCommands();
    return switch (listTab) {
      case COMMANDS -> macro.getCommands();
      case ON_ENABLE -> macro.getOnEnable();
      case ON_DISABLE -> macro.getOnDisable();
    };
  }

  private String listTabLabel(ListTab tab) {
    return switch (tab) {
      case COMMANDS -> "Commands";
      case ON_ENABLE -> "On Enable";
      case ON_DISABLE -> "On Disable";
    };
  }

  // Tabs

  /** Tab rects with wrapping; the plus tab always trails the macros. */
  private List<TabRect> layoutTabs() {
    List<TabRect> rects = new ArrayList<>();
    List<Macro> all = all();
    int tabY = y + TITLE_HEIGHT + 2;
    int tabX = x + 3;
    int maxX = x + width - 3;
    for (int i = 0; i <= all.size(); i++) {
      boolean plus = i == all.size();
      String text = plus ? "+" : all.get(i).getName();
      // The selected tab always shows its full name; background tabs truncate.
      if (!plus && i != selectedIndex && text.length() > 14) {
        text = text.substring(0, 13) + "..";
      }
      int w = FontUtil.getStringWidth(text) + 10;
      if (tabX + w > maxX && !rects.isEmpty()) {
        tabX = x + 3;
        tabY += TAB_HEIGHT + 2;
      }
      rects.add(new TabRect(text, tabX, tabY, Math.min(w, maxX - (x + 3)), plus ? -1 : i, plus));
      tabX += Math.min(w, maxX - (x + 3)) + 2;
    }
    return rects;
  }

  private int tabsHeight(List<TabRect> rects) {
    int rows = 1;
    int lastY = -1;
    for (TabRect tab : rects) {
      if (tab.y() != lastY) {
        if (lastY != -1) rows++;
        lastY = tab.y();
      }
    }
    return rows * (TAB_HEIGHT + 2) - 2;
  }

  private boolean isTabSelected(TabRect tab) {
    if (tab.plus()) return selectedIndex < 0;
    return tab.macroIndex() == selectedIndex;
  }

  // Rendering

  @Override
  protected void renderContent(int mouseX, int mouseY, float partialTicks) {
    List<TabRect> tabs = layoutTabs();
    List<Macro> all = all();
    for (TabRect tab : tabs) {
      boolean hovered =
          mouseX >= tab.x()
              && mouseX <= tab.x() + tab.w()
              && mouseY >= tab.y()
              && mouseY <= tab.y() + TAB_HEIGHT;
      boolean active = isTabSelected(tab);
      boolean on =
          !tab.plus()
              && tab.macroIndex() < all.size()
              && all.get(tab.macroIndex()).getMode() == Macro.Mode.TOGGLE
              && all.get(tab.macroIndex()).isEnabled();
      int bg = active ? Colors.getClientColorCustomAlpha(160) : (hovered ? 0xFF3A3A3A : 0xFF222222);
      RenderMethods.drawRect(tab.x(), tab.y(), tab.x() + tab.w(), tab.y() + TAB_HEIGHT, bg);
      FontUtil.drawString(
          tab.text(),
          tab.x() + 5,
          tab.y() + 2,
          on ? 0xFF55FF55 : (active ? 0xFFFFFFFF : 0xFFBBBBBB));
    }

    int contentY = y + TITLE_HEIGHT + 2 + tabsHeight(tabs);
    Macro selected = selected();
    if (selected == null) {
      renderCreateView(mouseX, mouseY, contentY);
    } else {
      renderMacroView(mouseX, mouseY, contentY, selected);
    }

    String current = status();
    if (current != null) {
      FontUtil.drawString(
          current,
          x + width / 2 - FontUtil.getStringWidth(current) / 2,
          y + height - 12,
          0xFFFFFFFF);
    }
  }

  private void renderCreateView(int mouseX, int mouseY, int contentY) {
    FontUtil.drawString("New macro name:", x + 5, contentY + 2, 0xFFBBBBBB);
    drawInput(contentY + 12, nameBuffer, Field.NAME, "walk, autowalk...");
    int btnY = contentY + 12 + INPUT_HEIGHT + 6;
    createButtons.drawButton(0, btnY, mouseX, mouseY, "Create");
  }

  private void renderMacroView(int mouseX, int mouseY, int contentY, Macro macro) {
    int infoY = contentY + 2;
    infoButtons.drawButton(0, infoY, mouseX, mouseY, modeLabel(macro));
    infoButtons.drawButton(1, infoY, mouseX, mouseY, keyLabel(macro));
    infoButtons.drawButton(2, infoY, mouseX, mouseY, fireLabel(macro));
    infoButtons.drawButton(3, infoY, mouseX, mouseY, "Delete");

    int listTabY = listTabY();
    List<ListTab> tabs = listTabs(macro);
    int tabX = x + 3;
    for (int i = 0; i < tabs.size(); i++) {
      ListTab tab = tabs.get(i);
      String label =
          (tab == listTab ? "[" + listTabLabel(tab) + "]" : listTabLabel(tab))
              + (i < tabs.size() - 1 || true ? "" : "");
      String full = (tab == listTab ? "[" : "") + listTabLabel(tab) + (tab == listTab ? "]" : "");
      int w = FontUtil.getStringWidth(full) + 10;
      boolean hovered =
          mouseX >= tabX
              && mouseX <= tabX + w
              && mouseY >= listTabY
              && mouseY <= listTabY + TAB_HEIGHT;
      RenderMethods.drawRect(
          tabX,
          listTabY,
          tabX + w,
          listTabY + TAB_HEIGHT,
          tab == listTab
              ? Colors.getClientColorCustomAlpha(160)
              : (hovered ? 0xFF3A3A3A : 0xFF222222));
      FontUtil.drawString(full, tabX + 5, listTabY + 2, 0xFFEEEEEE);
      tabX += w + 2;
    }
    String addLabel = "+ Add";
    int addW = FontUtil.getStringWidth(addLabel) + 10;
    boolean addHovered =
        mouseX >= tabX
            && mouseX <= tabX + addW
            && mouseY >= listTabY
            && mouseY <= listTabY + TAB_HEIGHT;
    RenderMethods.drawRect(
        tabX, listTabY, tabX + addW, listTabY + TAB_HEIGHT, addHovered ? 0xFF3A3A3A : 0xFF222222);
    FontUtil.drawString(addLabel, tabX + 5, listTabY + 2, 0xFFEEEEEE);

    List<String> lines = visibleLines(macro);
    int listY = listTabY + TAB_HEIGHT + 3;
    int listBottom = linesBottom();
    int maxVisible = Math.max(1, (listBottom - listY) / ENTRY_HEIGHT);
    scrollOffset = Math.min(scrollOffset, Math.max(0, lines.size() - maxVisible));
    int rowY = listY;
    for (int i = scrollOffset; i < Math.min(lines.size(), scrollOffset + maxVisible); i++) {
      boolean hovered =
          mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT;
      if (i == selectedLine || hovered) {
        RenderMethods.drawRect(x, rowY, x + width, rowY + ENTRY_HEIGHT, 0x20FFFFFF);
      }
      FontUtil.drawString(
          (i == selectedLine ? "> " : "  ") + lines.get(i),
          x + 5,
          rowY + 2,
          i == selectedLine ? 0xFFFFFFAA : 0xFFBBBBBB);
      rowY += ENTRY_HEIGHT;
    }
    if (lines.isEmpty()) {
      FontUtil.drawString("  (empty — type below and press Enter)", x + 5, listY + 2, 0xFF666666);
    }

    int cmdY = y + height - INPUT_HEIGHT - 3;
    drawInput(cmdY, commandBuffer, Field.COMMAND, "command...");
  }

  private List<ListTab> listTabs(Macro macro) {
    if (macro.getMode() == Macro.Mode.INSTANT) return List.of(ListTab.COMMANDS);
    return List.of(ListTab.COMMANDS, ListTab.ON_ENABLE, ListTab.ON_DISABLE);
  }

  private String modeLabel(Macro macro) {
    return macro.getMode() == Macro.Mode.INSTANT ? "Mode: Instant" : "Mode: Toggle";
  }

  private String keyLabel(Macro macro) {
    if (capturing) return "press key...";
    return "Key: " + KeyNames.name(macro.getKey());
  }

  private String fireLabel(Macro macro) {
    if (macro.getMode() == Macro.Mode.INSTANT) return "Fire";
    return macro.isEnabled() ? "Disable" : "Enable";
  }

  private void drawInput(int inputY, StringBuilder buffer, Field field, String hint) {
    RenderMethods.drawRect(x + 3, inputY, x + width - 3, inputY + INPUT_HEIGHT, 0xFF222222);
    boolean active = isFocused() && focusedField == field;
    if (buffer.length() == 0 && !active) {
      FontUtil.drawString(hint, x + 6, inputY + 3, 0xFF666666);
    } else {
      long now = System.currentTimeMillis();
      if (now - lastBlinkTime >= 500) {
        lastBlinkTime = now;
        cursorVisible = !cursorVisible;
      }
      String text = buffer.toString();
      int maxChars = 48;
      String shown =
          text.length() > maxChars ? "..." + text.substring(text.length() - maxChars) : text;
      FontUtil.drawString(shown, x + 6, inputY + 3, 0xFFEEEEEE);
      if (cursorVisible && active) {
        int cursorX = x + 6 + FontUtil.getStringWidth(shown);
        RenderMethods.drawRect(cursorX, inputY + 2, cursorX + 1, inputY + 12, 0xFFEEEEEE);
      }
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

  // Buttons

  private final WindowButtons infoButtons =
      new WindowButtons(this, 4, BUTTON_HEIGHT, id -> selected() != null);
  private final WindowButtons createButtons = new WindowButtons(this, 1, BUTTON_HEIGHT, id -> true);

  private void drawButton(
      WindowButtons buttons, int id, int btnY, int mouseX, int mouseY, String label) {
    buttons.drawButton(id, btnY, mouseX, mouseY, label);
  }

  // Input

  private int createContentY() {
    return y + TITLE_HEIGHT + 2 + tabsHeight(layoutTabs());
  }

  private boolean inNameInput(int mouseX, int mouseY) {
    int inputY = createContentY() + 12;
    return mouseX >= x + 3
        && mouseX <= x + width - 3
        && mouseY >= inputY
        && mouseY <= inputY + INPUT_HEIGHT;
  }

  private boolean inCommandInput(int mouseX, int mouseY) {
    int cmdY = y + height - INPUT_HEIGHT - 3;
    return mouseX >= x + 3
        && mouseX <= x + width - 3
        && mouseY >= cmdY
        && mouseY <= cmdY + INPUT_HEIGHT;
  }

  @Override
  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    if (button != InputConstants.MOUSE_BUTTON_LEFT) {
      return false;
    }
    for (TabRect tab : layoutTabs()) {
      if (mouseX >= tab.x()
          && mouseX <= tab.x() + tab.w()
          && mouseY >= tab.y()
          && mouseY <= tab.y() + TAB_HEIGHT) {
        if (tab.plus()) {
          selectedIndex = -1;
          focusedField = Field.NAME;
        } else if (selectedIndex != tab.macroIndex()) {
          selectedIndex = tab.macroIndex();
          selectedLine = -1;
          listTab = ListTab.COMMANDS;
          scrollOffset = 0;
        }
        capturing = false;
        return true;
      }
    }
    Macro selected = selected();
    if (selected == null) {
      if (inNameInput(mouseX, mouseY)) {
        focusedField = Field.NAME;
        return true;
      }
      int btnY = createContentY() + 12 + INPUT_HEIGHT + 3;
      if (createButtons.clickButton(0, btnY, mouseX, mouseY)) {
        createFromInput();
        return true;
      }
      return true;
    }
    if (inCommandInput(mouseX, mouseY)) {
      focusedField = Field.COMMAND;
      return true;
    }
    int contentY = createContentY();
    int infoY = contentY + 2;
    if (infoButtons.clickButton(0, infoY, mouseX, mouseY)) {
      cycleMode(selected);
      return true;
    }
    if (infoButtons.clickButton(1, infoY, mouseX, mouseY)) {
      capturing = true;
      return true;
    }
    if (infoButtons.clickButton(2, infoY, mouseX, mouseY)) {
      macros().fire(selected);
      return true;
    }
    if (infoButtons.clickButton(3, infoY, mouseX, mouseY)) {
      deleteMacro(selected);
      return true;
    }
    int hit = listTabHit(mouseX, mouseY, selected);
    if (hit == -2) {
      appendFromInput(selected);
      return true;
    }
    if (hit >= 0) {
      List<ListTab> tabs = listTabs(selected);
      listTab = tabs.get(hit);
      selectedLine = -1;
      focusedField = Field.COMMAND;
      return true;
    }
    int lineHit = lineHit(mouseX, mouseY, selected);
    if (lineHit >= -1) {
      selectedLine = selectedLine == lineHit ? -1 : lineHit;
      return true;
    }
    return true;
  }

  /** Y of the list-tab row; single source so clicks always match rendering. */
  private int listTabY() {
    return createContentY() + 2 + BUTTON_HEIGHT + 6;
  }

  /** Bottom of the command lines, just clear of the text entry. */
  private int linesBottom() {
    return y + height - INPUT_HEIGHT - 6;
  }

  /** -2 = the + Add button, >= 0 = list tab index, -1 = nothing. */
  private int listTabHit(int mouseX, int mouseY, Macro macro) {
    int listTabY = listTabY();
    if (mouseY < listTabY || mouseY > listTabY + TAB_HEIGHT) return -1;
    int tabX = x + 3;
    List<ListTab> tabs = listTabs(macro);
    for (int i = 0; i < tabs.size(); i++) {
      String full =
          (tabs.get(i) == listTab ? "[" : "")
              + listTabLabel(tabs.get(i))
              + (tabs.get(i) == listTab ? "]" : "");
      int w = FontUtil.getStringWidth(full) + 10;
      if (mouseX >= tabX && mouseX <= tabX + w) return i;
      tabX += w + 2;
    }
    int addW = FontUtil.getStringWidth("+ Add") + 10;
    if (mouseX >= tabX && mouseX <= tabX + addW) return -2;
    return -1;
  }

  /** Visible line index under the mouse, or -1. */
  private int lineHit(int mouseX, int mouseY, Macro macro) {
    List<String> lines = visibleLines(macro);
    int listTabY = listTabY();
    int listY = listTabY + TAB_HEIGHT + 3;
    int listBottom = linesBottom();
    int maxVisible = Math.max(1, (listBottom - listY) / ENTRY_HEIGHT);
    for (int i = scrollOffset; i < Math.min(lines.size(), scrollOffset + maxVisible); i++) {
      int rowY = listY + (i - scrollOffset) * ENTRY_HEIGHT;
      if (mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT) {
        return i;
      }
    }
    return -1;
  }

  @Override
  protected boolean consumeScroll(double mouseX, double mouseY, double scrollDelta) {
    Macro selected = selected();
    int count = selected == null ? 0 : visibleLines(selected).size();
    int listTabY = listTabY();
    int listY = listTabY + TAB_HEIGHT + 3;
    int listBottom = linesBottom();
    int maxVisible = Math.max(1, (listBottom - listY) / ENTRY_HEIGHT);
    int maxScroll = Math.max(0, count - maxVisible);
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
    if (capturing) {
      if (key == InputConstants.KEY_ESCAPE) {
        capturing = false;
        return true;
      }
      Macro selected = selected();
      if (selected != null) {
        selected.setKey(key);
        macros().saveMacros();
        setStatus("§aBound to " + KeyNames.name(key), 3000);
      }
      capturing = false;
      return true;
    }
    if (key == InputConstants.KEY_RETURN) {
      if (focusedField == Field.NAME) {
        createFromInput();
      } else {
        appendFromInput(selected());
      }
      return true;
    }
    if (key == InputConstants.KEY_BACKSPACE) {
      StringBuilder buffer = focusedField == Field.NAME ? nameBuffer : commandBuffer;
      if (buffer.length() > 0) {
        buffer.deleteCharAt(buffer.length() - 1);
      }
      return true;
    }
    if (key == InputConstants.KEY_ESCAPE) {
      nameBuffer.setLength(0);
      commandBuffer.setLength(0);
      selectedLine = -1;
      return true;
    }
    if (key == InputConstants.KEY_DELETE) {
      deleteSelectedLine();
      return true;
    }
    if (key == InputConstants.KEY_V && Minecraft.getInstance().hasControlDown()) {
      String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
      if (clip != null) {
        (focusedField == Field.NAME ? nameBuffer : commandBuffer).append(clip.trim());
      }
      return true;
    }
    return appendKeyChar(focusedField == Field.NAME ? nameBuffer : commandBuffer, event, 96);
  }

  // Actions

  private void createFromInput() {
    String text = nameBuffer.toString().trim();
    if (text.isEmpty()) {
      return;
    }
    if (macros().getMacro(text) != null) {
      setStatus("§cMacro exists (" + text + ")", 3000);
      return;
    }
    Macro macro = new Macro(text);
    macros().add(macro);
    nameBuffer.setLength(0);
    selectedIndex = macros().getRegistry().size() - 1;
    selectedLine = -1;
    listTab = ListTab.COMMANDS;
    focusedField = Field.COMMAND;
    setStatus("§aAdded macro (" + text + ")", 3000);
  }

  private void appendFromInput(Macro selected) {
    String text = commandBuffer.toString().trim();
    if (text.isEmpty()) return;
    if (selected == null) {
      setStatus("§cSelect a macro first", 3000);
      return;
    }
    List<String> lines =
        switch (listTab) {
          case COMMANDS -> selected.getCommands();
          case ON_ENABLE -> selected.getOnEnable();
          case ON_DISABLE -> selected.getOnDisable();
        };
    // Instant macros only keep the one command list.
    if (selected.getMode() == Macro.Mode.INSTANT && listTab != ListTab.COMMANDS) {
      setStatus("§cInstant macros use Commands", 3000);
      return;
    }
    lines.add(text);
    macros().saveMacros();
    commandBuffer.setLength(0);
    setStatus("§aAdded line", 2000);
  }

  private void deleteMacro(Macro macro) {
    macros().remove(macro.getName());
    setStatus("§7Removed (" + macro.getName() + ")", 3000);
    List<Macro> all = all();
    if (selectedIndex >= all.size()) {
      selectedIndex = all.isEmpty() ? -1 : 0;
    }
    selectedLine = -1;
  }

  private void deleteSelectedLine() {
    Macro selected = selected();
    if (selected == null || selectedLine < 0) return;
    List<String> lines = visibleLines(selected);
    if (selectedLine < lines.size()) {
      lines.remove(selectedLine);
      macros().saveMacros();
      setStatus("§7Removed line", 2000);
    }
    selectedLine = -1;
  }

  private void cycleMode(Macro macro) {
    Macro.Mode next =
        macro.getMode() == Macro.Mode.INSTANT ? Macro.Mode.TOGGLE : Macro.Mode.INSTANT;
    if (next == Macro.Mode.INSTANT && macro.isEnabled()) {
      macros().setEnabled(macro, false);
    }
    macro.setMode(next);
    if (next == Macro.Mode.INSTANT) {
      listTab = ListTab.COMMANDS;
    }
    selectedLine = -1;
    macros().saveMacros();
    setStatus("§aMode: " + next.name().toLowerCase(), 2000);
  }
}
