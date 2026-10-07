package me.friendly.exeter.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NotificationManager {

  public static final class Notification {
    private final String text;
    private final String iconName;
    private long createdAt;
    private final long durationMs;
    private int count;

    public Notification(String text, String iconName, long createdAt, long durationMs) {
      this.text = text;
      this.iconName = iconName;
      this.createdAt = createdAt;
      this.durationMs = durationMs;
      this.count = 1;
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

    public int count() {
      return count;
    }

    public String displayText() {
      return count > 1 ? text + " (x" + count + ")" : text;
    }
  }

  private static final List<Notification> queue = new ArrayList<>();
  private static final long DEFAULT_DURATION = 4000;
  private static int maxVisible = 5;

  private NotificationManager() {}

  /** Visible cap, driven by the Notifications HUD setting. */
  public static void setMaxVisible(int max) {
    maxVisible = Math.max(1, Math.min(10, max));
  }

  public static void push(String text, String iconName) {
    push(text, iconName, DEFAULT_DURATION);
  }

  public static void push(String text, String iconName, long durationMs) {
    synchronized (queue) {
      for (Notification n : queue) {
        if (n.text.equals(text)
            && n.iconName.equals(iconName)
            && System.currentTimeMillis() - n.createdAt < n.durationMs + 300) {
          n.count++;
          n.createdAt = System.currentTimeMillis();
          return;
        }
      }
      if (queue.size() >= 10) {
        // Evict with the exit animation instead of vanishing: rewind the oldest in
        // place so only its fade-out remains, letting it expire naturally.
        Notification oldest = queue.get(0);
        oldest.createdAt = System.currentTimeMillis() - oldest.durationMs;
      }
      queue.add(new Notification(text, iconName, System.currentTimeMillis(), durationMs));
      while (queue.size() > 12) {
        queue.remove(0);
      }
    }
  }

  public static List<Notification> getActive() {
    long now = System.currentTimeMillis();
    synchronized (queue) {
      Iterator<Notification> it = queue.iterator();
      while (it.hasNext()) {
        Notification n = it.next();
        if (now - n.createdAt() > n.durationMs() + 300) it.remove();
      }
      int from = Math.max(0, queue.size() - maxVisible);
      return new ArrayList<>(queue.subList(from, queue.size()));
    }
  }

  public static void clear() {
    synchronized (queue) {
      queue.clear();
    }
  }
}
