package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.util.NotificationManager;
import me.friendly.exeter.util.NotificationManager.Notification;

public final class NotificationHud extends HudModule {

  public NotificationHud() {
    super("Notifications", new String[] {"notifications", "notifs"}, Corner.BOTTOM_RIGHT);
    offerProperties();
  }

  @Override
  public int getWidth() {
    int width = 0;
    List<Notification> notifications = NotificationManager.getNotifications();
    int count = Math.min(notifications.size(), 5);
    for (int i = 0; i < count; i++) {
      int w = FontUtil.getStringWidth(notifications.get(i).text());
      if (w > width) {
        width = w;
      }
    }
    return width;
  }

  @Override
  public int getHeight() {
    int count = Math.min(NotificationManager.getNotifications().size(), 5);
    if (count == 0) {
      return 0;
    }
    return count * (FontUtil.getFontHeight() + 1) - 1;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    List<Notification> notifications = NotificationManager.getNotifications();
    int count = Math.min(notifications.size(), 5);
    int y = getY();
    for (int i = 0; i < count; i++) {
      Notification notification = notifications.get(i);
      String text = notification.text();
      int x = getX();
      if (getCorner() == Corner.TOP_RIGHT || getCorner() == Corner.BOTTOM_RIGHT) {
        x = getX() + getWidth() - FontUtil.getStringWidth(text);
      }
      FontUtil.drawString(text, (float) x, (float) y, 0xFFFFFFFF);
      y += FontUtil.getFontHeight() + 1;
    }
  }
}
