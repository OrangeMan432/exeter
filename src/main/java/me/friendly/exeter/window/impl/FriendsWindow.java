package me.friendly.exeter.window.impl;

import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.platform.InputConstants;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.friend.Friend;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.window.Window;
import net.minecraft.client.input.KeyEvent;

public class FriendsWindow extends Window {
  private static final int ENTRY_HEIGHT = 14;
  private static final int BUTTON_HEIGHT = 12;

  private final StringBuilder usernameBuffer = new StringBuilder();
  private final StringBuilder aliasBuffer = new StringBuilder();
  private int activeInput = 0;
  private int scrollOffset = 0;
  private boolean addingMode = false;

  public FriendsWindow(int x, int y, int width, int height) {
    super("Friends", x, y, width, height);
  }

  @Override
  protected void renderContent(int mouseX, int mouseY, float partialTicks) {
    int contentY = y + 16;
    int listHeight = height - 16 - 30;

    List<Friend> friends = new ArrayList<>(Exeter.getInstance().getFriendManager().getRegistry());
    int maxVisible = listHeight / ENTRY_HEIGHT;
    int maxScroll = Math.max(0, friends.size() - maxVisible);
    scrollOffset = Math.min(scrollOffset, maxScroll);

    int rowY = contentY + 2;
    for (int i = scrollOffset; i < Math.min(friends.size(), scrollOffset + maxVisible); i++) {
      Friend friend = friends.get(i);
      String display = friend.getAlias() + " (" + friend.getLabel() + ")";
      FontUtil.drawString(display, x + 5, rowY + 2, 0xFFEEEEEE);

      int removeX = x + width - 14;
      boolean removeHovered = mouseX >= removeX && mouseX <= removeX + 12
          && mouseY >= rowY && mouseY <= rowY + 12;
      int removeColor = removeHovered ? 0xFFFF5555 : 0xFF888888;
      FontUtil.drawString("X", removeX + 3, rowY + 2, removeColor);

      boolean rowHovered = mouseX >= x && mouseX <= x + width
          && mouseY >= rowY && mouseY <= rowY + ENTRY_HEIGHT;
      if (rowHovered) {
        RenderMethods.drawRect(x, rowY, x + width, rowY + ENTRY_HEIGHT, 0x20FFFFFF);
      }

      rowY += ENTRY_HEIGHT;
    }

    RenderMethods.drawRect(x, contentY + listHeight, x + width, y + height, 0xFF111111);

    int addBtnX = x + 3;
    int addBtnY = contentY + listHeight + 3;
    boolean addHovered = mouseX >= addBtnX && mouseX <= addBtnX + 30
        && mouseY >= addBtnY && mouseY <= addBtnY + BUTTON_HEIGHT;
    RenderMethods.drawRect(addBtnX, addBtnY, addBtnX + 30, addBtnY + BUTTON_HEIGHT,
        addHovered ? Colors.getClientColorCustomAlpha(180) : Colors.getClientColorCustomAlpha(120));
    FontUtil.drawString("Add", addBtnX + 7, addBtnY + 2, 0xFFFFFFFF);

    if (addingMode) {
      int inputY = contentY + listHeight + 3;
      int inputX = addBtnX + 35;

      FontUtil.drawString("Name:", inputX, inputY + 2, 0xFFAAAAAA);
      int nameFieldX = inputX + 30;
      RenderMethods.drawRect(nameFieldX, inputY, nameFieldX + 60, inputY + BUTTON_HEIGHT, 0xFF222222);
      String nameText = usernameBuffer.toString();
      FontUtil.drawString(nameText + (activeInput == 0 ? "_" : ""), nameFieldX + 3, inputY + 2, 0xFFEEEEEE);

      FontUtil.drawString("Alias:", nameFieldX + 65, inputY + 2, 0xFFAAAAAA);
      int aliasFieldX = nameFieldX + 95;
      RenderMethods.drawRect(aliasFieldX, inputY, aliasFieldX + 60, inputY + BUTTON_HEIGHT, 0xFF222222);
      String aliasText = aliasBuffer.toString();
      FontUtil.drawString(aliasText + (activeInput == 1 ? "_" : ""), aliasFieldX + 3, inputY + 2, 0xFFEEEEEE);
    }

    if (friends.isEmpty() && !addingMode) {
      FontUtil.drawString("No friends added.", x + width / 2 - 40, contentY + listHeight / 2, 0xFF888888);
    }
  }

  @Override
  protected boolean consumeClick(int mouseX, int mouseY, int button) {
    if (button != 0) return false;

    int contentY = y + 16;
    int listHeight = height - 16 - 30;

    List<Friend> friends = new ArrayList<>(Exeter.getInstance().getFriendManager().getRegistry());
    int maxVisible = listHeight / ENTRY_HEIGHT;

    for (int i = scrollOffset; i < Math.min(friends.size(), scrollOffset + maxVisible); i++) {
      int rowY = contentY + 2 + (i - scrollOffset) * ENTRY_HEIGHT;
      int removeX = x + width - 14;
      if (mouseX >= removeX && mouseX <= removeX + 12
          && mouseY >= rowY && mouseY <= rowY + 12) {
        Friend friend = friends.get(i);
        Exeter.getInstance().getFriendManager().unregister(friend);
        return true;
      }
    }

    int addBtnX = x + 3;
    int addBtnY = contentY + listHeight + 3;
    if (mouseX >= addBtnX && mouseX <= addBtnX + 30
        && mouseY >= addBtnY && mouseY <= addBtnY + BUTTON_HEIGHT) {
      addingMode = !addingMode;
      activeInput = 0;
      return true;
    }

    if (addingMode) {
      int nameFieldX = addBtnX + 65;
      int aliasFieldX = nameFieldX + 95;
      int inputY = contentY + listHeight + 3;

      if (mouseX >= nameFieldX && mouseX <= nameFieldX + 60
          && mouseY >= inputY && mouseY <= inputY + BUTTON_HEIGHT) {
        activeInput = 0;
        return true;
      }
      if (mouseX >= aliasFieldX && mouseX <= aliasFieldX + 60
          && mouseY >= inputY && mouseY <= inputY + BUTTON_HEIGHT) {
        activeInput = 1;
        return true;
      }
    }

    return true;
  }

  @Override
  protected boolean consumeKeyPress(KeyEvent event) {
    if (!addingMode) return false;
    int key = event.key();

    if (key == InputConstants.KEY_RETURN) {
      String username = usernameBuffer.toString().trim();
      String alias = aliasBuffer.toString().trim();
      if (!username.isEmpty() && !alias.isEmpty()) {
        if (!Exeter.getInstance().getFriendManager().isFriend(username)) {
          Exeter.getInstance().getFriendManager().register(new Friend(username, alias));
        }
        usernameBuffer.setLength(0);
        aliasBuffer.setLength(0);
      }
      return true;
    }

    if (key == InputConstants.KEY_BACKSPACE) {
      StringBuilder active = activeInput == 0 ? usernameBuffer : aliasBuffer;
      if (active.length() > 0) {
        active.deleteCharAt(active.length() - 1);
      }
      return true;
    }

    if (key == InputConstants.KEY_TAB) {
      activeInput = 1 - activeInput;
      return true;
    }

    int codepoint = event.keycode();
    if (codepoint >= 32 && codepoint < 127) {
      StringBuilder active = activeInput == 0 ? usernameBuffer : aliasBuffer;
      if (active.length() < 16) {
        active.append((char) codepoint);
      }
      return true;
    }

    return true;
  }

  @Override
  protected boolean consumeScroll(double mouseX, double mouseY, double scrollDelta) {
    List<Friend> friends = Exeter.getInstance().getFriendManager().getRegistry();
    int listHeight = height - 16 - 30;
    int maxVisible = listHeight / ENTRY_HEIGHT;
    int maxScroll = Math.max(0, friends.size() - maxVisible);

    if (scrollDelta < 0) {
      scrollOffset = Math.min(maxScroll, scrollOffset + 1);
    } else {
      scrollOffset = Math.max(0, scrollOffset - 1);
    }
    return true;
  }
}
