package me.friendly.exeter.module.impl.toggle.render.clickgui;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.active.render.Colors;
import com.mojang.blaze3d.platform.InputConstants;


public class SearchSelectPopup {

  public interface ToggleItem {
    String getLabel();

    boolean isEnabled();

    void setEnabled(boolean enabled);
  }

  private final String title;
  private final List<ToggleItem> items;
  private final List<ToggleItem> filtered = new ArrayList<>();
  private final Runnable onDone;
  private final Runnable onCancel;

  private String search = "";
  private int scroll = 0;
  private boolean focused = false;
  private long lastCursorBlink = System.currentTimeMillis();
  private boolean cursorVisible = true;

  private boolean draggingScrollbar = false;
  private int dragOffsetY;

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
  private static final int SCROLLBAR_W = 2;
  private static final int SCROLLBAR_PAD = 2;

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

    RenderMethods.drawRect(0, 0, screenW, screenH, 0x80000000);

    RenderMethods.drawGradientRect(
        popupX,
        popupY - 1.5f,
        popupX + popupW,
        popupY + HEADER_H - 6,
        Colors.getClientColorCustomAlpha(77),
        Colors.getClientColorCustomAlpha(77));

    RenderMethods.drawRect(
        popupX,
        popupY + HEADER_H - 6,
        popupX + popupW,
        popupY + popupH,
        0x77000000);

    FontUtil.drawString(title, popupX + 6, popupY + 1.5f, 0xFFFFFFFF);

    boolean searchHover =
        mouseX >= popupX + 4
            && mouseX <= popupX + popupW - 4
            && mouseY >= popupY + SEARCH_Y_OFFSET
            && mouseY <= popupY + SEARCH_Y_OFFSET + SEARCH_H;
    RenderMethods.drawRect(
        popupX + 4,
        popupY + SEARCH_Y_OFFSET,
        popupX + popupW - 4,
        popupY + SEARCH_Y_OFFSET + SEARCH_H,
        focused ? 0xFF444444 : (searchHover ? 0xFF3A3A3A : 0xFF2A2A2A));

    String displayText = search.isEmpty() ? "Search..." : search;
    int textColor = search.isEmpty() ? 0xFF888888 : 0xFFFFFFFF;
    FontUtil.drawString(displayText, popupX + 8, popupY + SEARCH_Y_OFFSET + 3, textColor);

    if (focused) {
      long now = System.currentTimeMillis();
      if (now - lastCursorBlink > 500) {
        cursorVisible = !cursorVisible;
        lastCursorBlink = now;
      }
      if (cursorVisible) {
        int cursorX = popupX + 8 + FontUtil.getStringWidth(search);
        RenderMethods.drawRect(
            cursorX, popupY + SEARCH_Y_OFFSET + 2, cursorX + 1,
            popupY + SEARCH_Y_OFFSET + SEARCH_H - 1, 0xFFFFFFFF);
      }
    }

    RenderMethods.drawRect(popupX + 4, listY, popupX + popupW - 4, listY + listH, 0x77000000);

    int maxScroll = Math.max(0, filtered.size() * (itemH + 1) - listH);
    scroll = Math.max(0, Math.min(scroll, maxScroll));

    boolean hasScrollbar = maxScroll > 0;
    int contentRight = hasScrollbar ? popupX + popupW - SCROLLBAR_W - SCROLLBAR_PAD - 4 : popupX + popupW - 4;

    if (hasScrollbar && draggingScrollbar) {
      int trackH = listH;
      float contentH = filtered.size() * (itemH + 1);
      int thumbH = Math.max(10, (int) ((trackH / contentH) * trackH));
      int scrollRange = trackH - thumbH;
      float ratio = (float) (mouseY - listY - dragOffsetY) / scrollRange;
      ratio = Math.max(0, Math.min(1, ratio));
      scroll = (int) (ratio * maxScroll);
    }

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
          mouseX >= popupX + 4
              && mouseX <= contentRight
              && mouseY >= iy
              && mouseY <= iy + itemH;

      if (hover) {
        RenderMethods.drawRect(popupX + 4, drawTop, contentRight, drawBottom, 0xFF333333);
      }

      int textY = iy + 3;
      if (textY >= listY && textY + 8 <= listY + listH) {
        FontUtil.drawString(item.getLabel(), popupX + 10, textY, hover ? 0xFFFFFFFF : 0xFFCCCCCC);

        int toggleX = contentRight - 18;
        int toggleY = Math.max(iy + 3, listY);
        int toggleBottom = Math.min(iy + 11, listY + listH);
        boolean on = item.isEnabled();
        int toggleColor = on ? Colors.getClientColorCustomAlpha(200) : 0xFF555555;
        if (toggleBottom > toggleY) {
          RenderMethods.drawRect(toggleX, toggleY, toggleX + 14, toggleBottom, toggleColor);
        }
      }
    }

    if (hasScrollbar) {
      int scrollbarX = popupX + popupW - SCROLLBAR_W - SCROLLBAR_PAD - 2;
      int trackTop = listY;
      int trackH = listH;

      float contentH = filtered.size() * (itemH + 1);
      int thumbH = Math.max(10, (int) ((trackH / contentH) * trackH));
      int thumbY = trackTop + (int) (((float) scroll / maxScroll) * (trackH - thumbH));

      RenderMethods.drawRect(
          scrollbarX, trackTop, scrollbarX + SCROLLBAR_W, trackTop + trackH, 0x44000000);
      RenderMethods.drawRect(
          scrollbarX, thumbY, scrollbarX + SCROLLBAR_W, thumbY + thumbH, 0xFF888888);
    }

    int selectAllX = popupX + 4;
    int selectAllY = popupY + popupH - BUTTON_AREA_H;
    boolean saHover =
        mouseX >= selectAllX
            && mouseX <= selectAllX + 55
            && mouseY >= selectAllY
            && mouseY <= selectAllY + 14;
    RenderMethods.drawRect(
        selectAllX,
        selectAllY,
        selectAllX + 55,
        selectAllY + 14,
        saHover ? 0xFF444444 : 0xFF2A2A2A);
    FontUtil.drawString("Select All", selectAllX + 4, selectAllY + 3, 0xFFCCCCCC);

    int doneX = popupX + popupW - 59;
    int doneY = popupY + popupH - BUTTON_AREA_H;
    boolean doneHover =
        mouseX >= doneX
            && mouseX <= doneX + 55
            && mouseY >= doneY
            && mouseY <= doneY + 14;
    int doneColor =
        doneHover ? Colors.getClientColorCustomAlpha(180) : Colors.getClientColorCustomAlpha(120);
    RenderMethods.drawRect(doneX, doneY, doneX + 55, doneY + 14, doneColor);
    FontUtil.drawString("Done", doneX + 16, doneY + 3, 0xFFFFFFFF);
  }

  public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
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
    boolean inPopup =
        mouseX >= popupX
            && mouseX <= popupX + popupW
            && mouseY >= popupY
            && mouseY <= popupY + popupH;

    if (!inPopup) {
      return false;
    }

    if (inSearchBar && mouseButton == InputConstants.MOUSE_BUTTON_LEFT) {
      focused = true;
      return true;
    }

    focused = false;

    if (mouseButton == InputConstants.MOUSE_BUTTON_LEFT) {
      int selectAllX = popupX + 4;
      int selectAllY = popupY + popupH - BUTTON_AREA_H;
      if (mouseX >= selectAllX
          && mouseX <= selectAllX + 55
          && mouseY >= selectAllY
          && mouseY <= selectAllY + 14) {
        filtered.forEach(item -> item.setEnabled(true));
        return true;
      }

      int doneX = popupX + popupW - 59;
      int doneY = popupY + popupH - BUTTON_AREA_H;
      if (mouseX >= doneX
          && mouseX <= doneX + 55
          && mouseY >= doneY
          && mouseY <= doneY + 14) {
        onDone.run();
        return true;
      }

      int maxScroll = Math.max(0, filtered.size() * (itemH + 1) - listH);
      if (maxScroll > 0) {
        int scrollbarX = popupX + popupW - SCROLLBAR_W - SCROLLBAR_PAD - 2;
        int trackH = listH;
        float contentH = filtered.size() * (itemH + 1);
        int thumbH = Math.max(10, (int) ((trackH / contentH) * trackH));
        int thumbY = listY + (int) (((float) scroll / maxScroll) * (trackH - thumbH));

        boolean inThumb =
            mouseX >= scrollbarX
                && mouseX <= scrollbarX + SCROLLBAR_W
                && mouseY >= thumbY
                && mouseY <= thumbY + thumbH;
        boolean inTrack =
            mouseX >= scrollbarX
                && mouseX <= scrollbarX + SCROLLBAR_W
                && mouseY >= listY
                && mouseY <= listY + listH;

        if (inThumb) {
          draggingScrollbar = true;
          dragOffsetY = mouseY - thumbY;
          return true;
        } else if (inTrack) {
          int scrollRange = trackH - thumbH;
          float ratio = (float) (mouseY - listY - thumbH / 2) / scrollRange;
          ratio = Math.max(0, Math.min(1, ratio));
          scroll = (int) (ratio * maxScroll);
          draggingScrollbar = true;
          dragOffsetY = thumbH / 2;
          return true;
        }
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

    if (mouseButton == InputConstants.MOUSE_BUTTON_RIGHT) {
      int doneX = popupX + popupW - 59;
      int doneY = popupY + popupH - BUTTON_AREA_H;
      if (mouseX >= doneX
          && mouseX <= doneX + 55
          && mouseY >= doneY
          && mouseY <= doneY + 14) {
        onCancel.run();
        return true;
      }
    }

    return true;
  }

  public boolean mouseScrolled(double scrollDelta) {
    int maxScroll = Math.max(0, filtered.size() * (itemH + 1) - listH);
    scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (scrollDelta * 12)));
    return true;
  }

  public boolean mouseReleased(int mouseX, int mouseY, int button) {
    draggingScrollbar = false;
    return true;
  }

  public boolean mouseDragged(int mouseX, int mouseY) {
    if (draggingScrollbar) {
      int maxScroll = Math.max(0, filtered.size() * (itemH + 1) - listH);
      if (maxScroll > 0) {
        int trackH = listH;
        float contentH = filtered.size() * (itemH + 1);
        int thumbH = Math.max(10, (int) ((trackH / contentH) * trackH));
        int scrollRange = trackH - thumbH;
        float ratio = (float) (mouseY - listY - dragOffsetY) / scrollRange;
        ratio = Math.max(0, Math.min(1, ratio));
        scroll = (int) (ratio * maxScroll);
      }
      return true;
    }
    return false;
  }

  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == InputConstants.KEY_ESCAPE) {
      onCancel.run();
      return true;
    }
    if (keyCode == InputConstants.KEY_BACKSPACE && focused) {
      if (!search.isEmpty()) {
        search = search.substring(0, search.length() - 1);
      }
      updateFiltered();
      return true;
    }
    return true;
  }

  public boolean charTyped(char c, int modifiers) {
    if (!focused) return false;
    if (c >= 32 && c < 127) {
      search += c;
      updateFiltered();
      return true;
    }
    return false;
  }

  private void updateFiltered() {
    filtered.clear();
    if (search.isEmpty()) {
      filtered.addAll(items);
    } else {
      String lower = search.toLowerCase();
      for (ToggleItem item : items) {
        if (item.getLabel().toLowerCase().contains(lower)) {
          filtered.add(item);
        }
      }
    }
    scroll = 0;
  }
}
