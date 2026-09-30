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
import net.minecraft.client.render.Tessellator;
import org.lwjgl.opengl.GL11;

/** Garry's Mod style notifications with slide-in/out animation. */
public final class NotificationHud extends HudModule {

  private static final int NOTIF_HEIGHT = 22;
  private static final int NOTIF_PADDING = 6;
  private static final int ICON_SIZE = 16;
  private static final int IN_DURATION = 300;
  private static final int OUT_DURATION = 300;
  private static final int BG_COLOR = 0xCC2A2A2A;
  private static final int RADIUS = 4;
  private static final int MAX_VISIBLE = 5;

  public NotificationHud() {
    super("Notifications", new String[] {"notifications", "notif", "notify"}, Corner.BOTTOM_RIGHT);
    setDescription("Garry's Mod style notifications.");
    offerProperties();
  }

  private static float easeOutCubic(float t) {
    return 1.0F - (float) Math.pow(1.0 - t, 3);
  }

  private static float easeInCubic(float t) {
    return t * t * t;
  }

  private List<Notification> entries() {
    List<Notification> notifications = NotificationManager.getNotifications();
    if (notifications.size() > MAX_VISIBLE) {
      notifications = notifications.subList(
          notifications.size() - MAX_VISIBLE, notifications.size());
    }
    if (!notifications.isEmpty()) return notifications;
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc != null && mc.currentScreen instanceof HudEditorScreen) {
      // Editor preview is a real notification through the normal render path.
      List<Notification> preview = new ArrayList<Notification>();
      preview.add(new Notification("Example notification", "info", 5000L));
      return preview;
    }
    return notifications;
  }

  private int entryWidth(Notification n) {
    return FontUtil.getStringWidth(n.text()) + NOTIF_PADDING * 2 + ICON_SIZE + 4;
  }

  @Override
  public int getWidth() {
    int width = 0;
    for (Notification n : entries()) {
      int w = entryWidth(n);
      if (w > width) width = w;
    }
    return width;
  }

  @Override
  public int getHeight() {
    int count = entries().size();
    if (count == 0) return 0;
    return count * (NOTIF_HEIGHT + 2) - 2;
  }

  private void rect(int x, int y, int x1, int y1, int color) {
    float a = ((color >> 24) & 0xFF) / 255.0F;
    float r = ((color >> 16) & 0xFF) / 255.0F;
    float g = ((color >> 8) & 0xFF) / 255.0F;
    float b = (color & 0xFF) / 255.0F;
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glColor4f(r, g, b, a);
    Tessellator tess = Tessellator.INSTANCE;
    tess.start(7);
    tess.vertex((double) x, (double) y1, 0.0);
    tess.vertex((double) x1, (double) y1, 0.0);
    tess.vertex((double) x1, (double) y, 0.0);
    tess.vertex((double) x, (double) y, 0.0);
    tess.draw();
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_BLEND);
  }

  private void drawRoundedRect(int x, int y, int x1, int y1, int radius, int color) {
    int w = x1 - x;
    int h = y1 - y;
    int r = Math.min(radius, Math.min(w / 2, h / 2));
    if (r <= 0) {
      rect(x, y, x1, y1, color);
      return;
    }
    int baseAlpha = (color >> 24) & 0xFF;
    int rgb = color & 0x00FFFFFF;
    rect(x + r, y, x1 - r, y1, color);
    rect(x, y + r, x + r, y1 - r, color);
    rect(x1 - r, y + r, x1, y1 - r, color);
    for (int py = 0; py < r; py++) {
      for (int px = 0; px < r; px++) {
        double dx = r - px - 0.5;
        double dy = r - py - 0.5;
        double dist = Math.sqrt(dx * dx + dy * dy);
        int a;
        if (dist <= r - 0.5) a = baseAlpha;
        else if (dist >= r + 0.5) continue;
        else a = (int) (baseAlpha * (r + 0.5 - dist));
        if (a <= 0) continue;
        int col = (a << 24) | rgb;
        rect(x + px, y + py, x + px + 1, y + py + 1, col);
        rect(x1 - 1 - px, y + py, x1 - px, y + py + 1, col);
        rect(x + px, y1 - 1 - py, x + px + 1, y1 - py, col);
        rect(x1 - 1 - px, y1 - 1 - py, x1 - px, y1 - py, col);
      }
    }
  }

  private void drawIcon(String iconName, int x, int y) {
    int col = 0xFF3B82F6;
    if (iconName != null) {
      String lower = iconName.toLowerCase();
      if (lower.contains("error") || lower.contains("cross") || lower.contains("cancel")) {
        col = 0xFFEF4444;
      } else if (lower.contains("success") || lower.contains("tick") || lower.contains("accept")) {
        col = 0xFF22C55E;
      } else if (lower.contains("warn") || lower.contains("exclam") || lower.contains("caution")) {
        col = 0xFFF59E0B;
      } else if (lower.contains("undo")) {
        col = 0xFF3B82F6;
      } else if (lower.contains("clean")) {
        col = 0xFF06B6D4;
      } else if (lower.contains("hint")) {
        col = 0xFFA855F7;
      }
    }
    rect(x, y, x + ICON_SIZE, y + ICON_SIZE, col | 0xFF000000);
    rect(x + 1, y + 1, x + ICON_SIZE - 1, y + ICON_SIZE - 1, 0xFF1A1A1A);
    String letter =
        (iconName != null && !iconName.isEmpty()) ? iconName.substring(0, 1).toUpperCase() : "!";
    int tw = FontUtil.getStringWidth(letter);
    FontUtil.drawString(letter, (float) (x + (ICON_SIZE - tw) / 2), (float) (y + 4), 0xFFFFFFFF);
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    List<Notification> active = entries();
    if (active.isEmpty()) return;

    long now = System.currentTimeMillis();
    boolean top = getCorner() == Corner.TOP_LEFT || getCorner() == Corner.TOP_RIGHT;
    boolean right = getCorner() == Corner.TOP_RIGHT || getCorner() == Corner.BOTTOM_RIGHT;

    for (int i = 0; i < active.size(); i++) {
      Notification n = active.get(i);
      long age = now - n.createdAt();
      float offsetX = 0.0F;
      int alpha = 0xCC;

      if (age < IN_DURATION) {
        float t = (float) age / IN_DURATION;
        offsetX = (1.0F - easeOutCubic(t)) * (getWidth() + 20);
      } else if (age > n.durationMs() - OUT_DURATION) {
        float t = (float) (age - (n.durationMs() - OUT_DURATION)) / OUT_DURATION;
        offsetX = easeInCubic(t) * (getWidth() + 20);
        alpha = (int) (0xCC * (1.0F - t));
        if (alpha < 0) alpha = 0;
      }

      int w = entryWidth(n);
      int h = NOTIF_HEIGHT;
      int y = top ? getY() + i * (h + 2) : getY() + getHeight() - h - i * (h + 2);

      int x;
      if (right) {
        x = getX() + getWidth() - w + (int) offsetX;
      } else {
        x = getX() - (int) offsetX;
      }

      int bg = (alpha << 24) | (BG_COLOR & 0x00FFFFFF);
      drawRoundedRect(x, y, x + w, y + h, RADIUS, bg);
      drawIcon(n.iconName(), x + 4, y + 3);

      int textColor;
      if (age > n.durationMs() - OUT_DURATION) {
        textColor = (alpha << 24) | 0x00FFFFFF;
      } else {
        textColor = 0xFFFFFFFF;
      }
      FontUtil.drawString(n.text(), (float) (x + ICON_SIZE + 8), (float) (y + 7), textColor);
    }
  }

  public static void push(String text, String icon) {
    NotificationManager.push(text, icon);
  }
}
