package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.hud.HudEditorScreen;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.util.NotificationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class NotificationHud extends HudModule {

  private static final int NOTIF_HEIGHT = 22;
  private static final int NOTIF_PADDING = 6;
  private static final int ICON_SIZE = 16;
  private static final int IN_DURATION = 300;
  private static final int OUT_DURATION = 300;
  private static final int BG_COLOR = 0xCC2A2A2A;
  private static final int RADIUS = 4;

  private final NumberProperty<Integer> maxNotifications =
      new NumberProperty<Integer>(5, 1, 10, "Max Notifications");

  public NotificationHud() {
    super("Notifications", new String[] {"notifications", "notif", "notify"}, Corner.BOTTOM_RIGHT);
    setDescription("Garry's Mod style notifications.");
    this.offerProperties(maxNotifications);
    maxNotifications.setDescription("Limits how many notifications are shown at once.");
  }

  private static float easeOutCubic(float t) {
    return 1.0f - (float) Math.pow(1.0 - t, 3);
  }

  private static float easeInCubic(float t) {
    return t * t * t;
  }

  private ItemStack iconToStack(String iconName) {
    if (iconName == null || iconName.isEmpty()) return null;
    try {
      String id = iconName.contains(":") ? iconName : "minecraft:" + iconName.toLowerCase();
      Identifier ident = Identifier.parse(id);
      var item = BuiltInRegistries.ITEM.getValue(ident);
      if (item != null && BuiltInRegistries.ITEM.getKey(item) != null) {
        // check if valid (not air)
        if (!item.toString().equals("air") && !ident.getPath().equals("air")) {
          return new ItemStack(item);
        }
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  private void drawRoundedRect(int x, int y, int x1, int y1, int radius, int color) {
    if (RenderMethods.guiGraphics == null) return;
    int w = x1 - x;
    int h = y1 - y;
    int r = Math.min(radius, Math.min(w / 2, h / 2));
    if (r <= 0) {
      RenderMethods.guiGraphics.fill(x, y, x1, y1, color);
      return;
    }
    int baseAlpha = (color >> 24) & 0xFF;
    int rgb = color & 0x00FFFFFF;
    // central
    RenderMethods.guiGraphics.fill(x + r, y, x1 - r, y1, color);
    RenderMethods.guiGraphics.fill(x, y + r, x + r, y1 - r, color);
    RenderMethods.guiGraphics.fill(x1 - r, y + r, x1, y1 - r, color);
    // anti-aliased corners
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
        // top-left
        RenderMethods.guiGraphics.fill(x + px, y + py, x + px + 1, y + py + 1, col);
        // top-right
        RenderMethods.guiGraphics.fill(x1 - 1 - px, y + py, x1 - px, y + py + 1, col);
        // bottom-left
        RenderMethods.guiGraphics.fill(x + px, y1 - 1 - py, x + px + 1, y1 - py, col);
        // bottom-right
        RenderMethods.guiGraphics.fill(x1 - 1 - px, y1 - 1 - py, x1 - px, y1 - py, col);
      }
    }
    // fill remaining corner interiors that were not covered by AA (center of corner)
    // the loop above already handles full opacity for inner pixels, but the rectangular
    // strips x+r interior already covers, so corners are complete
  }

  private void drawIcon(String iconName, int x, int y) {
    // try minecraft item first (icons like totem_of_undying, ender_pearl, rail)
    if (iconName != null && !iconName.isEmpty()) {
      ItemStack stack = iconToStack(iconName);
      if (stack != null && !stack.isEmpty()) {
        try {
          RenderMethods.guiGraphics.item(stack, x, y);
          return;
        } catch (Exception ignored) {
        }
      }
    }
    // try GMod silk icon16 (eye, exclamation, cross, tick, error, information, clock, arrow_undo)
    String silk = mapToSilk(iconName);
    if (silk != null && RenderMethods.guiGraphics != null) {
      try {
        Identifier id = Identifier.parse("exeter:textures/icon16/" + silk);
        // check resource exists
        var res = Minecraft.getInstance().getResourceManager().getResource(id);
        if (res.isPresent()) {
          RenderMethods.guiGraphics.blit(
              id, x, y, x + ICON_SIZE, y + ICON_SIZE, 0.0f, 1.0f, 0.0f, 1.0f);
          return;
        }
      } catch (Exception ignored) {
      }
    }
    // fallback: colored square with icon initial
    int col = 0xFF3B82F6;
    if (iconName != null) {
      String lower = iconName.toLowerCase();
      if (lower.contains("error") || lower.contains("cross") || lower.contains("cancel"))
        col = 0xFFEF4444;
      else if (lower.contains("success") || lower.contains("tick") || lower.contains("accept"))
        col = 0xFF22C55E;
      else if (lower.contains("warn") || lower.contains("exclam") || lower.contains("caution"))
        col = 0xFFF59E0B;
      else if (lower.contains("undo")) col = 0xFF3B82F6;
      else if (lower.contains("clean")) col = 0xFF06B6D4;
      else if (lower.contains("hint")) col = 0xFFA855F7;
    }
    if (RenderMethods.guiGraphics != null) {
      RenderMethods.guiGraphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, col | 0xFF000000);
      RenderMethods.guiGraphics.fill(
          x + 1, y + 1, x + ICON_SIZE - 1, y + ICON_SIZE - 1, 0xFF1A1A1A);
      String letter =
          (iconName != null && !iconName.isEmpty()) ? iconName.substring(0, 1).toUpperCase() : "!";
      int tw = FontUtil.getStringWidth(letter);
      FontUtil.drawString(letter, x + (ICON_SIZE - tw) / 2, y + 4, 0xFFFFFFFF);
    }
  }

  private String mapToSilk(String iconName) {
    if (iconName == null) return null;
    String lower = iconName.toLowerCase();
    return switch (lower) {
      case "eye", "eye_closed" -> "eye.png";
      case "exclamation", "warning", "warn", "caution" -> "exclamation.png";
      case "cross", "error", "cancel" -> "cross.png";
      case "tick", "accept", "success" -> "tick.png";
      case "information", "info", "hint" -> "information.png";
      case "clock" -> "clock.png";
      case "undo", "arrow_undo" -> "arrow_undo.png";
      default -> {
        // try direct + .png if file exists among our 10
        String cand = lower + ".png";
        if (cand.equals("eye.png")
            || cand.equals("exclamation.png")
            || cand.equals("cross.png")
            || cand.equals("tick.png")
            || cand.equals("accept.png")
            || cand.equals("error.png")
            || cand.equals("information.png")
            || cand.equals("clock.png")
            || cand.equals("arrow_undo.png")
            || cand.equals("cancel.png")) yield cand;
        yield null;
      }
    };
  }

  @Override
  public int getWidth() {
    NotificationManager.setMaxVisible(maxNotifications.getValue());
    List<NotificationManager.Notification> active = NotificationManager.getActive();
    // show dummy in editor when empty
    boolean inEditor =
        Minecraft.getInstance().gui != null
            && Minecraft.getInstance().gui.screen() instanceof HudEditorScreen;
    if (active.isEmpty() && inEditor) {
      String dummy = "Undone Prop";
      return FontUtil.getStringWidth(dummy) + NOTIF_PADDING * 2 + ICON_SIZE + 4;
    }
    int max = 0;
    for (var n : active) {
      int w = FontUtil.getStringWidth(n.displayText()) + NOTIF_PADDING * 2 + ICON_SIZE + 4;
      if (w > max) max = w;
    }
    return max;
  }

  @Override
  public int getHeight() {
    NotificationManager.setMaxVisible(maxNotifications.getValue());
    List<NotificationManager.Notification> active = NotificationManager.getActive();
    boolean inEditor =
        Minecraft.getInstance().gui != null
            && Minecraft.getInstance().gui.screen() instanceof HudEditorScreen;
    if (active.isEmpty() && inEditor) return NOTIF_HEIGHT;
    if (active.isEmpty()) return 0;
    return active.size() * (NOTIF_HEIGHT + 2) - 2;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    NotificationManager.setMaxVisible(maxNotifications.getValue());
    List<NotificationManager.Notification> active = NotificationManager.getActive();
    boolean inEditor =
        Minecraft.getInstance().gui != null
            && Minecraft.getInstance().gui.screen() instanceof HudEditorScreen;

    if (active.isEmpty() && inEditor) {
      // dummy preview
      int w = FontUtil.getStringWidth("Undone Prop") + NOTIF_PADDING * 2 + ICON_SIZE + 4;
      int x = getX();
      int y = getY();
      if (isRight()) {
        x = getX() + getWidth() - w;
      }
      drawRoundedRect(x, y, x + w, y + NOTIF_HEIGHT, RADIUS, BG_COLOR);
      drawIcon("undo", x + 4, y + 3);
      FontUtil.drawString("Undone Prop", x + ICON_SIZE + 8, y + 7, 0xFFFFFFFF);
      return;
    }
    if (active.isEmpty()) return;

    long now = System.currentTimeMillis();
    boolean top = isTop();
    boolean right = isRight();

    int baseY = getY();
    // stack direction: if top, downwards; if bottom, upwards
    for (int i = 0; i < active.size(); i++) {
      var n = active.get(i);
      long age = now - n.createdAt();
      float offsetX = 0;
      int alpha = 0xCC;

      if (age < IN_DURATION) {
        float t = (float) age / IN_DURATION;
        offsetX = (1.0f - easeOutCubic(t)) * (getWidth() + 20);
        alpha = 0xCC;
      } else if (age > n.durationMs() - OUT_DURATION) {
        float t = (float) (age - (n.durationMs() - OUT_DURATION)) / OUT_DURATION;
        offsetX = easeInCubic(t) * (getWidth() + 20);
        alpha = (int) (0xCC * (1.0f - t));
        if (alpha < 0) alpha = 0;
      }

      int w = FontUtil.getStringWidth(n.displayText()) + NOTIF_PADDING * 2 + ICON_SIZE + 4;
      int h = NOTIF_HEIGHT;

      int y = top ? baseY + i * (h + 2) : baseY + getHeight() - h - i * (h + 2);

      int x = getX();
      if (right) {
        x = getX() + getWidth() - w + (int) offsetX;
      } else {
        x = getX() - (int) offsetX;
      }

      int bg = (alpha << 24) | (BG_COLOR & 0x00FFFFFF);
      drawRoundedRect(x, y, x + w, y + h, RADIUS, bg);

      // icon
      drawIcon(n.iconName(), x + 4, y + 3);

      // text with alpha
      int textColor = (alpha << 24) | 0x00FFFFFF;
      // if fading, reduce alpha for text as well
      if (age > n.durationMs() - OUT_DURATION) {
        textColor = (alpha << 24) | 0x00FFFFFF;
      } else {
        textColor = 0xFFFFFFFF;
      }
      FontUtil.drawString(n.displayText(), x + ICON_SIZE + 8, y + 7, textColor);
    }
  }

  public static void push(String text, String icon) {
    NotificationManager.push(text, icon);
  }
}
