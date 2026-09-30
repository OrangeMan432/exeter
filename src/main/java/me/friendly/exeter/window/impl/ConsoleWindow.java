package me.friendly.exeter.window.impl;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.logging.LogEntry;
import me.friendly.exeter.logging.Logger;
import me.friendly.exeter.window.Window;

public class ConsoleWindow extends Window {
  private static final int INPUT_HEIGHT = 14;
  private static final int LINE_HEIGHT = 11;
  private static final int MAX_ENTRIES = 500;

  private static final List<LogEntry> staticEntries = new ArrayList<LogEntry>();
  private static boolean listenerRegistered = false;

  private final StringBuilder inputBuffer = new StringBuilder();
  private boolean cursorVisible = true;
  private long lastBlinkTime = System.currentTimeMillis();

  public ConsoleWindow(int x, int y, int width, int height) {
    super("Console", x, y, width, height);
    if (!listenerRegistered) {
      Logger.getLogger()
          .addListener(
              new java.util.function.Consumer<LogEntry>() {
                @Override
                public void accept(LogEntry entry) {
                  staticEntries.add(entry);
                  while (staticEntries.size() > MAX_ENTRIES) {
                    staticEntries.remove(0);
                  }
                }
              });
      listenerRegistered = true;
    }
  }

  @Override
  protected void renderContent(int mouseX, int mouseY, float partialTicks) {
    int contentY = y + TITLE_HEIGHT;
    int contentHeight = height - TITLE_HEIGHT - INPUT_HEIGHT;

    fill(x, contentY + contentHeight, x + width, contentY + contentHeight + INPUT_HEIGHT, 0x77282828);
    FontUtil.drawString(">", x + 3.0f, contentY + contentHeight + 3.0f, 0xFF888888);

    String inputText = inputBuffer.toString();
    long now = System.currentTimeMillis();
    if (now - lastBlinkTime >= 500) {
      lastBlinkTime = now;
      cursorVisible = !cursorVisible;
    }
    FontUtil.drawString(inputText, x + 10.0f, contentY + contentHeight + 3.0f, 0xFFEEEEEE);
    if (cursorVisible && isFocused()) {
      int cursorX = x + 10 + FontUtil.getStringWidth(inputText);
      fill(cursorX, contentY + contentHeight + 2, cursorX + 1, contentY + contentHeight + 12,
          0xFFEEEEEE);
    }

    int maxLines = Math.max(1, contentHeight / LINE_HEIGHT);
    List<String> wrapped = new ArrayList<String>();
    List<Integer> colors = new ArrayList<Integer>();
    int maxWidth = width - 6;
    for (int i = 0; i < staticEntries.size(); i++) {
      String line = staticEntries.get(i).getMessage();
      int color = 0xFFAAAAAA;
      if (line.contains("[WARNING]")) color = 0xFFFFFF55;
      if (line.contains("[ERROR]")) color = 0xFFFF5555;
      if (line.startsWith(">")) color = 0xFF55FF55;
      List<String> parts = wrapLine(line, maxWidth);
      for (int j = 0; j < parts.size(); j++) {
        wrapped.add(parts.get(j));
        colors.add(Integer.valueOf(color));
      }
    }

    int startIndex = Math.max(0, wrapped.size() - maxLines);
    int endIndex = wrapped.size();

    int lineY = contentY + 2;
    for (int i = startIndex; i < endIndex; i++) {
      FontUtil.drawString(wrapped.get(i), x + 3.0f, (float) lineY,
          colors.get(i).intValue());
      lineY += LINE_HEIGHT;
    }
  }

  private List<String> wrapLine(String text, int maxWidth) {
    List<String> lines = new ArrayList<String>();
    String[] words = text.split(" ", -1);
    StringBuilder current = new StringBuilder();
    for (int i = 0; i < words.length; i++) {
      String word = words[i];
      String candidate =
          current.length() == 0 ? word : current.toString() + " " + word;
      if (FontUtil.getStringWidth(candidate) <= maxWidth || current.length() == 0) {
        current.setLength(0);
        current.append(candidate);
      } else {
        lines.add(current.toString());
        current.setLength(0);
        current.append(word);
      }
    }
    String rest = current.toString();
    while (!rest.isEmpty() && FontUtil.getStringWidth(rest) > maxWidth) {
      int cut = rest.length() - 1;
      while (cut > 1 && FontUtil.getStringWidth(rest.substring(0, cut)) > maxWidth) {
        cut--;
      }
      lines.add(rest.substring(0, cut));
      rest = rest.substring(cut);
    }
    lines.add(rest);
    return lines;
  }

  private void addLine(String text, LogEntry.Level level) {
    staticEntries.add(new LogEntry(text, level));
    while (staticEntries.size() > MAX_ENTRIES) {
      staticEntries.remove(0);
    }
  }

  @Override
  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    return true;
  }

  @Override
  protected boolean consumeKeyTyped(char typedChar, int keyCode) {
    if (keyCode == 28) {
      String input = inputBuffer.toString().trim();
      inputBuffer.setLength(0);
      if (!input.isEmpty()) {
        addLine("> " + input, LogEntry.Level.OUTPUT);
        if (Exeter.getInstance() != null) {
          String effective = input;
          String prefix = Exeter.getInstance().getCommandManager().getPrefix();
          if (effective.startsWith(prefix)) {
            effective = effective.substring(prefix.length());
          }
          if (!effective.trim().isEmpty()) {
            String response =
                Exeter.getInstance().getCommandManager().dispatchDirect(effective);
            if (response != null && !response.isEmpty()) {
              addLine(response, LogEntry.Level.INFO);
            }
          }
        }
      }
      return true;
    }
    if (keyCode == 14) {
      if (inputBuffer.length() > 0) {
        inputBuffer.deleteCharAt(inputBuffer.length() - 1);
      }
      return true;
    }
    if (typedChar >= 32 && typedChar < 127) {
      inputBuffer.append(typedChar);
      return true;
    }
    return false;
  }
}
