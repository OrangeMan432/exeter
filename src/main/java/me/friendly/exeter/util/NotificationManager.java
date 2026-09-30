package me.friendly.exeter.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NotificationManager {

  public static final class Notification {
    private final String text;
    private final String iconName;
    private final long createdAt;
    private final long durationMs;

    public Notification(String text, String iconName, long durationMs) {
      this.text = text;
      this.iconName = iconName;
      this.createdAt = System.currentTimeMillis();
      this.durationMs = durationMs;
    }

    public String text() {
      return text;
    }

    public String iconName() {
      return iconName;
    }

    public long createdAt() {
      return createdAt;
    }

    public long durationMs() {
      return durationMs;
    }

    public boolean isExpired() {
      return System.currentTimeMillis() - createdAt >= durationMs;
    }
  }

  private static final List<Notification> queue = new ArrayList<Notification>();
  private static final long DEFAULT_DURATION = 4000;

  private NotificationManager() {}

  public static void push(String text, String iconName) {
    push(text, iconName, DEFAULT_DURATION);
  }

  public static synchronized void push(String text, String iconName, long durationMs) {
    queue.add(new Notification(text, iconName, durationMs));
    while (queue.size() > 20) {
      queue.remove(0);
    }
  }

  public static synchronized List<Notification> getNotifications() {
    Iterator<Notification> it = queue.iterator();
    while (it.hasNext()) {
      if (it.next().isExpired()) {
        it.remove();
      }
    }
    return new ArrayList<Notification>(queue);
  }
}
