package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.properties.EnumProperty;

public final class WatermarkHud extends HudModule {

  public enum DisplayMode {
    FULL,
    SHORT,
    NAME
  }

  private final EnumProperty<DisplayMode> displayMode =
      new EnumProperty<DisplayMode>(DisplayMode.FULL, "Display", "display", "mode");

  public WatermarkHud() {
    super("Watermark", new String[] {"watermark", "wm", "water"}, Corner.TOP_LEFT);
    setDescription("Displays the client name and version.");
    this.offerProperties(displayMode);
    displayMode.setDescription("Chooses how much version detail the watermark shows.");
  }

  private String versionText() {
    String dirty = Exeter.DIRTY ? " (dirty)" : "";
    switch (displayMode.getValue()) {
      case SHORT:
        // Name and tag only: "Exeter b28", without the mc version and commit count.
        String build = Exeter.BUILD;
        int mcIndex = build.indexOf("-mc");
        return " " + (mcIndex == -1 ? build : build.substring(0, mcIndex));
      case NAME:
        return "";
      case FULL:
      default:
        return String.format(" %s.%s%s", Exeter.BUILD, Exeter.HASH, dirty);
    }
  }

  @Override
  public int getWidth() {
    return FontUtil.getStringWidth(Exeter.TITLE) + FontUtil.getStringWidth(versionText());
  }

  @Override
  public int getHeight() {
    return 9;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    FontUtil.drawString(Exeter.TITLE, getX(), getY(), Colors.getHudMain());
    FontUtil.drawString(
        versionText(),
        getX() + FontUtil.getStringWidth(Exeter.TITLE),
        getY(),
        Colors.getHudAccent());
  }
}
