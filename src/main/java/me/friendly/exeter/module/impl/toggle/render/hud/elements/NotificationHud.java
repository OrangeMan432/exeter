package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.module.impl.toggle.render.hud.HudEditorScreen;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.util.NotificationManager;
import me.friendly.exeter.util.NotificationManager.Notification;
import net.minecraft.client.Minecraft;

public final class NotificationHud extends HudModule {

  public NotificationHud() {
    super("Notifications", new String[] {"notifications", "notifs"}, Corner.BOTTOM_RIGHT);
    offerProperties();
  }

  private List<Notification> entries() {
    List<Notification> notifications = NotificationManager.getNotifications();
    if (!notifications.isEmpty()) return notifications;
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc != null && mc.currentScreen instanceof HudEditorScreen) {
      // Editor preview is a real notification through the normal render path.
      List<Notification> preview = new ArrayList<Notification>();
      preview.add(new Notification("Example notification", "", 5000L));
      return preview;
    }
    return notifications;
  }

  @Override
  public int getWidth() {
    int width = 0;
    List<Notification> notifications = entries();
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
    int count = Math.min(entries().size(), 5);
    if (count == 0) {
      return 0;
    }
    return count * (FontUtil.getFontHeight() + 1) - 1;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    List<Notification> notifications = entries();
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
