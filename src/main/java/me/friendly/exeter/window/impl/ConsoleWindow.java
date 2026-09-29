package me.friendly.exeter.window.impl;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.logging.LogEntry;
import me.friendly.exeter.logging.Logger;
import me.friendly.exeter.window.Window;
import net.minecraft.client.input.KeyEvent;

public class ConsoleWindow extends Window implements java.util.function.Consumer<LogEntry> {
  private static final int INPUT_HEIGHT = 14;
  private static final int LINE_HEIGHT = 11;
  private static final int MAX_ENTRIES = 500;

  private static final List<LogEntry> staticEntries = new ArrayList<>();
  private static boolean listenerRegistered = false;

  private final List<String> renderedLines = new ArrayList<>();
  private final StringBuilder inputBuffer = new StringBuilder();
  private int scrollOffset = 0;
  private boolean cursorVisible = true;
  private long lastBlinkTime = System.currentTimeMillis();
  private boolean autoScroll = true;

  public ConsoleWindow(int x, int y, int width, int height) {
    super("Console", x, y, width, height);
    if (!listenerRegistered) {
      Logger.getLogger()
          .addListener(
              entry -> {
                staticEntries.add(entry);
                while (staticEntries.size() > MAX_ENTRIES) {
                  staticEntries.remove(0);
                }
              });
      listenerRegistered = true;
    }
    rebuildLines();
  }

  private void rebuildLines() {
    renderedLines.clear();
    int maxWidth = width - 6;
    for (LogEntry entry : staticEntries) {
      wrapText(entry.getMessage(), maxWidth);
    }
  }

  private void wrapText(String text, int maxWidth) {
    if (FontUtil.getStringWidth(text) <= maxWidth) {
      renderedLines.add(text);
      return;
    }

    String[] words = text.split("(?<=\\s)");
    StringBuilder currentLine = new StringBuilder();
    for (String word : words) {
      String test = currentLine.toString() + word;
      if (FontUtil.getStringWidth(test) > maxWidth && currentLine.length() > 0) {
        renderedLines.add(currentLine.toString());
        currentLine = new StringBuilder(word);
      } else {
        currentLine.append(word);
      }
    }
    if (currentLine.length() > 0) {
      renderedLines.add(currentLine.toString());
    }
  }

  @Override
  protected void renderContent(int mouseX, int mouseY, float partialTicks) {
    rebuildLines();

    int contentY = y + 16;
    int contentHeight = height - 16 - INPUT_HEIGHT;

    RenderMethods.drawRect(
        x,
        contentY + contentHeight,
        x + width,
        contentY + contentHeight + INPUT_HEIGHT,
        0x77282828);
    FontUtil.drawString(">", x + 3, contentY + contentHeight + 3, 0xFF888888);

    String inputText = inputBuffer.toString();
    long now = System.currentTimeMillis();
    if (now - lastBlinkTime >= 500) {
      lastBlinkTime = now;
      cursorVisible = !cursorVisible;
    }

    String displayInput = inputText;
    int maxInputWidth = width - 12;
    if (FontUtil.getStringWidth(displayInput) > maxInputWidth) {
      int charCount = displayInput.length();
      while (charCount > 0
          && FontUtil.getStringWidth(displayInput.substring(displayInput.length() - charCount))
              > maxInputWidth) {
        charCount--;
      }
      displayInput = "..." + displayInput.substring(displayInput.length() - charCount);
    }

    FontUtil.drawString(displayInput, x + 10, contentY + contentHeight + 3, 0xFFEEEEEE);
    if (cursorVisible && isFocused()) {
      int cursorX = x + 10 + FontUtil.getStringWidth(displayInput);
      RenderMethods.drawRect(
          cursorX,
          contentY + contentHeight + 2,
          cursorX + 1,
          contentY + contentHeight + 12,
          0xFFEEEEEE);
    }

    int maxLines = contentHeight / LINE_HEIGHT;
    int startIndex = Math.max(0, scrollOffset);
    int endIndex = Math.min(renderedLines.size(), startIndex + maxLines);

    int lineY = contentY + 2;
    for (int i = startIndex; i < endIndex; i++) {
      String line = renderedLines.get(i);
      int color = parseLineColor(line);
      String cleanLine = stripColorCodes(line);
      FontUtil.drawString(cleanLine, x + 3, lineY, color);
      lineY += LINE_HEIGHT;
    }
  }

  private int parseLineColor(String line) {
    if (line.startsWith("[ERROR]")) return 0xFFFF5555;
    if (line.startsWith("[WARNING]")) return 0xFFFFFF55;
    if (line.startsWith(">")) return 0xFF55FF55;

    int lastColorCode = getLastColorCode(line);
    if (lastColorCode != -1) {
      return colorCodeToRGB((char) lastColorCode);
    }
    return 0xFFAAAAAA;
  }

  private int getLastColorCode(String line) {
    for (int i = line.length() - 2; i >= 0; i--) {
      if (line.charAt(i) == '\u00a7' || line.charAt(i) == '&') {
        char code = line.charAt(i + 1);
        if (isColorCode(code)) {
          return code;
        }
      }
    }
    return -1;
  }

  private boolean isColorCode(char c) {
    return "0123456789abcdefklmnor".indexOf(c) != -1;
  }

  private int colorCodeToRGB(char code) {
    return switch (code) {
      case '0' -> 0xFF000000;
      case '1' -> 0xFF0000AA;
      case '2' -> 0xFF00AA00;
      case '3' -> 0xFF00AAAA;
      case '4' -> 0xFFAA0000;
      case '5' -> 0xFFAA00AA;
      case '6' -> 0xFFAA5500;
      case '7' -> 0xFFAAAAAA;
      case '8' -> 0xFF555555;
      case '9' -> 0xFF5555FF;
      case 'a' -> 0xFF55FF55;
      case 'b' -> 0xFF55FFFF;
      case 'c' -> 0xFFFF5555;
      case 'd' -> 0xFFFF55FF;
      case 'e' -> 0xFFFFFF55;
      case 'f' -> 0xFFFFFFFF;
      default -> 0xFFAAAAAA;
    };
  }

  private String stripColorCodes(String text) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      if ((text.charAt(i) == '\u00a7' || text.charAt(i) == '&') && i + 1 < text.length()) {
        if (isColorCode(text.charAt(i + 1))) {
          i++;
          continue;
        }
      }
      sb.append(text.charAt(i));
    }
    return sb.toString();
  }

  private int getMaxVisibleLines() {
    int contentHeight = height - 16 - INPUT_HEIGHT;
    return contentHeight / LINE_HEIGHT;
  }

  @Override
  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    return true;
  }

  @Override
  protected boolean consumeScroll(double mouseX, double mouseY, double scrollDelta) {
    int maxScroll = Math.max(0, renderedLines.size() - getMaxVisibleLines());

    if (scrollDelta < 0) {
      scrollOffset = Math.min(maxScroll, scrollOffset + 3);
    } else {
      scrollOffset = Math.max(0, scrollOffset - 3);
    }

    autoScroll = scrollOffset >= maxScroll;
    return true;
  }

  @Override
  protected boolean consumeKeyPress(KeyEvent event) {
    int key = event.key();

    if (key == InputConstants.KEY_RETURN) {
      executeCommand();
      return true;
    }
    if (key == InputConstants.KEY_BACKSPACE) {
      if (inputBuffer.length() > 0) {
        inputBuffer.deleteCharAt(inputBuffer.length() - 1);
      }
      return true;
    }
    if (key == InputConstants.KEY_DOWN) {
      int maxScroll = Math.max(0, renderedLines.size() - getMaxVisibleLines());
      scrollOffset = Math.min(maxScroll, scrollOffset + 1);
      autoScroll = scrollOffset >= maxScroll;
      return true;
    }
    if (key == InputConstants.KEY_UP) {
      scrollOffset = Math.max(0, scrollOffset - 1);
      autoScroll = false;
      return true;
    }
    if (key == InputConstants.KEY_END) {
      scrollOffset = Math.max(0, renderedLines.size() - getMaxVisibleLines());
      autoScroll = true;
      return true;
    }
    if (key == InputConstants.KEY_HOME) {
      scrollOffset = 0;
      autoScroll = false;
      return true;
    }

    int codepoint = event.key();
    if (codepoint >= 32 && codepoint < 127) {
      inputBuffer.append((char) codepoint);
      return true;
    }

    return false;
  }

  private void executeCommand() {
    String input = inputBuffer.toString().trim();
    inputBuffer.setLength(0);
    cursorVisible = true;
    lastBlinkTime = System.currentTimeMillis();

    if (input.isEmpty()) return;

    addEntry(new LogEntry("> " + input, LogEntry.Level.OUTPUT));

    try {
      String result =
          me.friendly.exeter.core.Exeter.getInstance().getCommandManager().dispatchDirect(input);
      if (result != null && !result.isEmpty()) {
        addEntry(new LogEntry(result, LogEntry.Level.OUTPUT));
      }
    } catch (Exception e) {
      addEntry(new LogEntry("[ERROR] " + e.getMessage(), LogEntry.Level.ERROR));
    }
  }

  private void addEntry(LogEntry entry) {
    staticEntries.add(entry);
    while (staticEntries.size() > MAX_ENTRIES) {
      staticEntries.remove(0);
    }
    rebuildLines();
    if (autoScroll) {
      scrollOffset = Math.max(0, renderedLines.size() - getMaxVisibleLines());
    }
  }

  @Override
  public void accept(LogEntry entry) {
    addEntry(entry);
  }
}
