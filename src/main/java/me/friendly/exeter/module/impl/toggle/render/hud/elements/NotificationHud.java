package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.io.InputStream;
import java.util.List;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.hud.HudEditorScreen;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.render.EspRenderManager;
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
  private static final int RADIUS = 4;
  private static final int BAR_HEIGHT = 2;
  private static final int GREEN_BAR = 0x01FF00;

  private final NumberProperty<Integer> maxNotifications =
      new NumberProperty<Integer>(5, 1, 10, "Max Notifications");
  private final me.friendly.exeter.properties.Property<Boolean> progressBar =
      new me.friendly.exeter.properties.Property<Boolean>(false, "Progress Bar", "progressBar");
  private final me.friendly.exeter.properties.Property<Boolean> greenBar =
      new me.friendly.exeter.properties.Property<Boolean>(false, "Green Bar", "greenBar");
  private final me.friendly.exeter.properties.Property<Boolean> rounded =
      new me.friendly.exeter.properties.Property<Boolean>(true, "Rounded", "rounded");
  private final NumberProperty<Integer> offset = new NumberProperty<Integer>(0, 0, 64, "Offset");
  private final NumberProperty<Integer> gap = new NumberProperty<Integer>(2, 0, 32, "Gap");
  private final me.friendly.exeter.properties.Property<Boolean> fade =
      new me.friendly.exeter.properties.Property<Boolean>(true, "Fade", "fade");
  private final me.friendly.exeter.properties.Property<Boolean> slide =
      new me.friendly.exeter.properties.Property<Boolean>(true, "Slide", "slide");
  private final me.friendly.exeter.properties.Property<Boolean> icons =
      new me.friendly.exeter.properties.Property<Boolean>(true, "Icons", "icons");
  private final me.friendly.exeter.properties.Property<Boolean> border =
      new me.friendly.exeter.properties.Property<Boolean>(false, "Border", "border");
  private final NumberProperty<Integer> bgRed = new NumberProperty<Integer>(42, 0, 255, "BG Red");
  private final NumberProperty<Integer> bgGreen =
      new NumberProperty<Integer>(42, 0, 255, "BG Green");
  private final NumberProperty<Integer> bgBlue = new NumberProperty<Integer>(42, 0, 255, "BG Blue");
  private final NumberProperty<Integer> bgAlpha =
      new NumberProperty<Integer>(204, 0, 255, "BG Alpha");
  private final me.friendly.exeter.properties.PopupProperty pickBackground =
      new me.friendly.exeter.properties.PopupProperty("Background Color", this::openColorPicker);

  public NotificationHud() {
    super("Notifications", new String[] {"notifications", "notif", "notify"}, Corner.BOTTOM_RIGHT);
    setDescription("Garry's Mod style notifications.");
    this.offerProperties(
        maxNotifications,
        progressBar,
        greenBar,
        rounded,
        offset,
        gap,
        fade,
        slide,
        icons,
        border,
        pickBackground,
        bgRed,
        bgGreen,
        bgBlue,
        bgAlpha);
    maxNotifications.setDescription("Limits how many notifications are shown at once.");
    progressBar.setDescription("Draw a shrinking bar showing time left before removal.");
    greenBar.setDescription("Use green for the progress bar instead of the client color.");
    greenBar.visibleWhen(() -> progressBar.getValue());
    rounded.setDescription("Round the corners of the notification panel.");
    offset.setDescription("Distance between the notifications and the screen edge.");
    gap.setDescription("Distance between notifications.");
    fade.setDescription("Fade notifications out when they expire.");
    slide.setDescription("Slide notifications in and out.");
    icons.setDescription("Show the icon next to the notification text.");
    border.setDescription("Draw a border around the notification panel.");
    bgRed.setDescription("Background color red channel.");
    bgGreen.setDescription("Background color green channel.");
    bgBlue.setDescription("Background color blue channel.");
    bgAlpha.setDescription("Background opacity, 0-255.");
    // The raw sliders stay persisted for the config, the picker replaces them in the UI.
    bgRed.visibleWhen(() -> false);
    bgGreen.visibleWhen(() -> false);
    bgBlue.visibleWhen(() -> false);
  }

  /** Opens the RGB picker on the notification background color. */
  private void openColorPicker() {
    me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui.getClickGui()
        .openPopup(
            new me.friendly.exeter.module.impl.toggle.render.clickgui.ColorPickerPopup(
                () -> backgroundColor(0xCC), this::setBackgroundColor, null));
  }

  /** Writes a picked color into the background channels, keeping the alpha slider. */
  private void setBackgroundColor(int picked) {
    bgRed.setValue((picked >> 16) & 0xFF);
    bgGreen.setValue((picked >> 8) & 0xFF);
    bgBlue.setValue(picked & 0xFF);
  }

  /** Background color, with the panel alpha scaled by the fade animation. */
  private int backgroundColor(int alpha) {
    int scaled = (int) Math.round(bgAlpha.getValue() * (alpha / (double) 0xCC));
    int rgb = (bgRed.getValue() << 16) | (bgGreen.getValue() << 8) | bgBlue.getValue();
    return (scaled << 24) | (rgb & 0x00FFFFFF);
  }

  /** Pixels the icon column takes, so the text can shift left when icons are off. */
  private int iconSpace() {
    return icons.getValue() ? ICON_SIZE : 0;
  }

  /** Panel width of one notification. */
  private int panelWidth(String text) {
    return FontUtil.getStringWidth(text) + NOTIF_PADDING * 2 + iconSpace() + 4;
  }

  private static float easeOutCubic(float t) {
    return 1.0f - (float) Math.pow(1.0 - t, 3);
  }

  private static float easeInCubic(float t) {
    return t * t * t;
  }

  private static final String[] SILK_ICONS = {
    "eye.png",
    "exclamation.png",
    "cross.png",
    "tick.png",
    "accept.png",
    "error.png",
    "information.png",
    "clock.png",
    "arrow_undo.png",
    "cancel.png"
  };
  private static boolean silkRegistered;

  private static final java.util.Set<String> silkLoaded = new java.util.HashSet<>();

  /** Raw PNGs no longer self-upload on snapshot-4; register like Panel does. */
  private static void ensureSilkRegistered() {
    if (silkRegistered) return;
    silkRegistered = true;
    var mc = Minecraft.getInstance();
    for (String silk : SILK_ICONS) {
      // Classloader-direct: mod resource packs are absent without fabric-api.
      try (InputStream in =
          NotificationHud.class.getResourceAsStream("/assets/exeter/textures/icon16/" + silk)) {
        if (in == null) {
          System.out.println("Missing silk icon in jar: " + silk);
          continue;
        }
        com.mojang.blaze3d.platform.NativeImage img =
            com.mojang.blaze3d.platform.NativeImage.read(in);
        mc.getTextureManager()
            .register(
                Identifier.parse("exeter:textures/icon16/" + silk),
                net.minecraft.client.renderer.texture.TextureResources.from2dImage(
                    () -> "exeter:" + silk, img));
        silkLoaded.add(silk);
      } catch (Exception e) {
        System.out.println("Failed to load silk icon " + silk + ": " + e.getMessage());
      }
    }
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

  /** Draws the panel: background, optional border, rounded or square. */
  private void drawPanel(int x, int y, int w, int h, int alpha) {
    int bg = backgroundColor(alpha);
    drawRoundedRect(x, y, x + w, y + h, rounded.getValue() ? RADIUS : 0, bg);
    if (border.getValue()) {
      int borderAlpha = (int) Math.round(bgAlpha.getValue() * (alpha / (double) 0xCC));
      int borderColor = (borderAlpha << 24) | (EspRenderManager.getClientColor() & 0x00FFFFFF);
      RenderMethods.guiGraphics.fill(x, y, x + w, y + 1, borderColor);
      RenderMethods.guiGraphics.fill(x, y + h - 1, x + w, y + h, borderColor);
      RenderMethods.guiGraphics.fill(x, y, x + 1, y + h, borderColor);
      RenderMethods.guiGraphics.fill(x + w - 1, y, x + w, y + h, borderColor);
    }
  }

  /**
   * Offset shifts the panel away from the screen edge it is snapped to. The stored position stays
   * the anchor and the shift is applied on the way out, so the setter takes the drawn position back
   * and the on screen clamp stays idempotent.
   */
  private int shiftX() {
    return isRight() ? -offset.getValue() : offset.getValue();
  }

  private int shiftY() {
    return isTop() ? offset.getValue() : -offset.getValue();
  }

  @Override
  public int getX() {
    return super.getX() + shiftX();
  }

  @Override
  public void setX(int x) {
    super.setX(x - shiftX());
  }

  @Override
  public int getY() {
    return super.getY() + shiftY();
  }

  @Override
  public void setY(int y) {
    super.setY(y - shiftY());
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

  /** Plain bar, anchored to the screen edge the HUD is snapped to. */
  private void drawBar(int x, int y, int w, int h, boolean right, int color) {
    if (RenderMethods.guiGraphics == null || w <= 0) return;
    RenderMethods.guiGraphics.fill(x, y, x + w, y + h, color);
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
    ensureSilkRegistered();
    if (silk != null && RenderMethods.guiGraphics != null && silkLoaded.contains(silk)) {
      try {
        Identifier id = Identifier.parse("exeter:textures/icon16/" + silk);
        RenderMethods.guiGraphics.blit(
            id, x, y, x + ICON_SIZE, y + ICON_SIZE, 0.0f, 1.0f, 0.0f, 1.0f);
        return;
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
      return panelWidth("Undone Prop");
    }
    int max = 0;
    for (var n : active) {
      int w = panelWidth(n.displayText());
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
    int g = gap.getValue();
    return active.size() * (NOTIF_HEIGHT + g) - g;
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
      int w = panelWidth("Undone Prop");
      int x = getX();
      int y = getY();
      if (isRight()) {
        x = getX() + getWidth() - w;
      }
      drawPanel(x, y, w, NOTIF_HEIGHT, 0xCC);
      if (icons.getValue()) drawIcon("undo", x + 4, y + 3);
      FontUtil.drawString("Undone Prop", x + iconSpace() + 8, y + 7, 0xFFFFFFFF);
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
        if (slide.getValue()) {
          offsetX = (1.0f - easeOutCubic(t)) * (getWidth() + 20);
        }
        alpha = 0xCC;
      } else if (age > n.durationMs() - OUT_DURATION) {
        float t = (float) (age - (n.durationMs() - OUT_DURATION)) / OUT_DURATION;
        if (slide.getValue()) {
          offsetX = easeInCubic(t) * (getWidth() + 20);
        }
        if (fade.getValue()) {
          alpha = (int) (0xCC * (1.0f - t));
          if (alpha < 0) alpha = 0;
        }
      }

      int w = panelWidth(n.displayText());
      int h = NOTIF_HEIGHT;
      int g = gap.getValue();

      int y = top ? baseY + i * (h + g) : baseY + getHeight() - h - i * (h + g);

      int x = getX();
      if (right) {
        x = getX() + getWidth() - w + (int) offsetX;
      } else {
        x = getX() - (int) offsetX;
      }

      drawPanel(x, y, w, h, alpha);

      // icon
      if (icons.getValue()) drawIcon(n.iconName(), x + 4, y + 3);

      // time remaining bar: shrinks towards the screen edge the HUD is snapped to
      if (progressBar.getValue() && RenderMethods.guiGraphics != null) {
        float remaining = 1.0f - (float) age / (float) Math.max(1, n.durationMs());
        remaining = Math.max(0f, Math.min(1f, remaining));
        int barWidth = Math.round(w * remaining);
        if (barWidth > 0) {
          int barY = y + h - BAR_HEIGHT;
          int barColor =
              (alpha << 24)
                  | ((greenBar.getValue() ? GREEN_BAR : EspRenderManager.getClientColor())
                      & 0x00FFFFFF);
          if (right) {
            drawBar(x + w - barWidth, barY, barWidth, BAR_HEIGHT, true, barColor);
          } else {
            drawBar(x, barY, barWidth, BAR_HEIGHT, false, barColor);
          }
        }
      }

      // text with alpha
      int textColor = (alpha << 24) | 0x00FFFFFF;
      // if fading, reduce alpha for text as well
      if (age > n.durationMs() - OUT_DURATION) {
        textColor = (alpha << 24) | 0x00FFFFFF;
      } else {
        textColor = 0xFFFFFFFF;
      }
      FontUtil.drawString(n.displayText(), x + iconSpace() + 8, y + 7, textColor);
    }
  }

  public static void push(String text, String icon) {
    NotificationManager.push(text, icon);
  }
}
