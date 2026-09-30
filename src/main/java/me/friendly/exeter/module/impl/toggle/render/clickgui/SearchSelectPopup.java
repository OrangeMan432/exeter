package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.active.render.Colors;
import net.minecraft.client.gui.DrawableHelper;

public class SearchSelectPopup extends DrawableHelper {

  public interface ToggleItem {
    String getLabel();

    boolean isEnabled();

    void setEnabled(boolean enabled);
  }

  private final String title;
  private final List<ToggleItem> items;
  private final List<ToggleItem> filtered = new ArrayList<ToggleItem>();
  private final Runnable onDone;
  private final Runnable onCancel;

  private String search = "";
  private int scroll = 0;
  private boolean focused = true;
  private long lastCursorBlink = System.currentTimeMillis();
  private boolean cursorVisible = true;

  private int popupX;
  private int popupY;
  private int popupW;
  private int popupH;
  private int listY;
  private int listH;
  private int itemH = 14;

  private static final int HEADER_H = 18;
  private static final int SEARCH_H = 14;
  private static final int SEARCH_GAP = 2;
  private static final int SEARCH_Y_OFFSET = HEADER_H + SEARCH_GAP;
  private static final int LIST_GAP = 4;
  private static final int LIST_Y_OFFSET = SEARCH_Y_OFFSET + SEARCH_H + LIST_GAP;
  private static final int BUTTON_AREA_H = 20;
  private static final int BUTTON_GAP = 4;

  public SearchSelectPopup(
      String title, List<ToggleItem> items, Runnable onDone, Runnable onCancel) {
    this.title = title;
    this.items = items;
    this.onDone = onDone;
    this.onCancel = onCancel;
    this.filtered.addAll(items);
  }

  public void render(int mouseX, int mouseY, float partialTicks, int screenW, int screenH) {
    popupW = 200;
    popupH = 240;
    popupX = (screenW - popupW) / 2;
    popupY = (screenH - popupH) / 2;

    listY = popupY + LIST_Y_OFFSET;
    listH = popupH - LIST_Y_OFFSET - BUTTON_AREA_H - BUTTON_GAP;

    fill(0, 0, screenW, screenH, 0x80000000);

    fillGradient(
        popupX,
        popupY - 1,
        popupX + popupW,
        popupY + HEADER_H - 6,
        Colors.getClientColorCustomAlpha(77),
        Colors.getClientColorCustomAlpha(77));

    fill(popupX, popupY + HEADER_H - 6, popupX + popupW, popupY + popupH, 0x77000000);

    FontUtil.drawString(title, popupX + 6.0f, popupY + 1.5f, 0xFFFFFFFF);

    fill(popupX + 4, popupY + SEARCH_Y_OFFSET, popupX + popupW - 4,
        popupY + SEARCH_Y_OFFSET + SEARCH_H, focused ? 0xFF444444 : 0xFF2A2A2A);

    String displayText = search.isEmpty() ? "Search..." : search;
    int textColor = search.isEmpty() ? 0xFF888888 : 0xFFFFFFFF;
    FontUtil.drawString(displayText, popupX + 8.0f, popupY + SEARCH_Y_OFFSET + 3.0f, textColor);

    if (focused) {
      long now = System.currentTimeMillis();
      if (now - lastCursorBlink > 500) {
        cursorVisible = !cursorVisible;
        lastCursorBlink = now;
      }
      if (cursorVisible) {
        int cursorX = popupX + 8 + FontUtil.getStringWidth(search);
        fill(cursorX, popupY + SEARCH_Y_OFFSET + 2, cursorX + 1,
            popupY + SEARCH_Y_OFFSET + SEARCH_H - 1, 0xFFFFFFFF);
      }
    }

    fill(popupX + 4, listY, popupX + popupW - 4, listY + listH, 0x77000000);

    int maxScroll = Math.max(0, filtered.size() * (itemH + 1) - listH);
    scroll = Math.max(0, Math.min(scroll, maxScroll));

    int contentRight = popupX + popupW - 4;
    int y = listY - scroll;

    for (int i = 0; i < filtered.size(); i++) {
      ToggleItem item = filtered.get(i);
      int iy = y + i * (itemH + 1);

      int drawTop = Math.max(iy, listY);
      int drawBottom = Math.min(iy + itemH, listY + listH);
      if (drawBottom <= drawTop) {
        continue;
      }

      boolean hover =
          mouseX >= popupX + 4 && mouseX <= contentRight && mouseY >= iy && mouseY <= iy + itemH;

      if (hover) {
        fill(popupX + 4, drawTop, contentRight, drawBottom, 0xFF333333);
      }

      int textY = iy + 3;
      if (textY >= listY && textY + 8 <= listY + listH) {
        FontUtil.drawString(item.getLabel(), popupX + 10.0f, (float) textY,
            hover ? 0xFFFFFFFF : 0xFFCCCCCC);

        int toggleX = contentRight - 18;
        int toggleY = Math.max(iy + 3, listY);
        int toggleBottom = Math.min(iy + 11, listY + listH);
        boolean on = item.isEnabled();
        int toggleColor = on ? Colors.getClientColorCustomAlpha(200) : 0xFF555555;
        if (toggleBottom > toggleY) {
          fill(toggleX, toggleY, toggleX + 14, toggleBottom, toggleColor);
        }
      }
    }

    int selectAllX = popupX + 4;
    int selectAllY = popupY + popupH - BUTTON_AREA_H;
    boolean saHover =
        mouseX >= selectAllX
            && mouseX <= selectAllX + 55
            && mouseY >= selectAllY
            && mouseY <= selectAllY + 14;
    fill(selectAllX, selectAllY, selectAllX + 55, selectAllY + 14,
        saHover ? 0xFF444444 : 0xFF2A2A2A);
    FontUtil.drawString("Select All", selectAllX + 4.0f, selectAllY + 3.0f, 0xFFCCCCCC);

    int doneX = popupX + popupW - 59;
    int doneY = popupY + popupH - BUTTON_AREA_H;
    boolean doneHover =
        mouseX >= doneX && mouseX <= doneX + 55 && mouseY >= doneY && mouseY <= doneY + 14;
    int doneColor =
        doneHover ? Colors.getClientColorCustomAlpha(180) : Colors.getClientColorCustomAlpha(120);
    fill(doneX, doneY, doneX + 55, doneY + 14, doneColor);
    FontUtil.drawString("Done", doneX + 16.0f, doneY + 3.0f, 0xFFFFFFFF);
  }

  public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
    boolean inPopup =
        mouseX >= popupX
            && mouseX <= popupX + popupW
            && mouseY >= popupY
            && mouseY <= popupY + popupH;

    if (!inPopup) {
      return false;
    }

    boolean inSearchBar =
        mouseX >= popupX + 4
            && mouseX <= popupX + popupW - 4
            && mouseY >= popupY + SEARCH_Y_OFFSET
            && mouseY <= popupY + SEARCH_Y_OFFSET + SEARCH_H;
    boolean inList =
        mouseX >= popupX + 4
            && mouseX <= popupX + popupW - 4
            && mouseY >= listY
            && mouseY <= listY + listH;

    if (inSearchBar && mouseButton == 0) {
      focused = true;
      return true;
    }

    focused = false;

    if (mouseButton == 0) {
      int selectAllX = popupX + 4;
      int selectAllY = popupY + popupH - BUTTON_AREA_H;
      if (mouseX >= selectAllX
          && mouseX <= selectAllX + 55
          && mouseY >= selectAllY
          && mouseY <= selectAllY + 14) {
        for (ToggleItem item : filtered) {
          item.setEnabled(true);
        }
        return true;
      }

      int doneX = popupX + popupW - 59;
      int doneY = popupY + popupH - BUTTON_AREA_H;
      if (mouseX >= doneX && mouseX <= doneX + 55 && mouseY >= doneY && mouseY <= doneY + 14) {
        onDone.run();
        return true;
      }

      if (inList) {
        int y = listY - scroll;
        for (int i = 0; i < filtered.size(); i++) {
          int iy = y + i * (itemH + 1);
          if (mouseY >= iy && mouseY <= iy + itemH) {
            ToggleItem item = filtered.get(i);
            item.setEnabled(!item.isEnabled());
            return true;
          }
        }
      }
    }

    if (mouseButton == 1) {
      int doneX = popupX + popupW - 59;
      int doneY = popupY + popupH - BUTTON_AREA_H;
      if (mouseX >= doneX && mouseX <= doneX + 55 && mouseY >= doneY && mouseY <= doneY + 14) {
        onCancel.run();
        return true;
      }
    }

    return true;
  }

  public boolean keyPressed(char typedChar, int keyCode) {
    if (keyCode == 1) {
      onCancel.run();
      return true;
    }
    if (keyCode == 14 && focused) {
      if (!search.isEmpty()) {
        search = search.substring(0, search.length() - 1);
      }
      updateFiltered();
      return true;
    }
    if (keyCode == 200) {
      scroll = Math.max(0, scroll - 14);
      return true;
    }
    if (keyCode == 208) {
      int maxScroll = Math.max(0, filtered.size() * (itemH + 1) - listH);
      scroll = Math.min(maxScroll, scroll + 14);
      return true;
    }
    if (focused && search.length() < 24) {
      if (typedChar >= 32 && typedChar < 127) {
        search += typedChar;
        updateFiltered();
        return true;
      }
    }
    return true;
  }

  private void updateFiltered() {
    filtered.clear();
    if (search.isEmpty()) {
      filtered.addAll(items);
    } else {
      filtered.addAll(FuzzySearch.filter(items, search, new java.util.function.Function<SearchSelectPopup.ToggleItem, String>() {
        @Override
        public String apply(SearchSelectPopup.ToggleItem item) {
          return item.getLabel();
        }
      }));
    }
    scroll = 0;
  }
}
