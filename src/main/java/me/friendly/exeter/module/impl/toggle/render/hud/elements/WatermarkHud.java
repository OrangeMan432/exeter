package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;

public final class WatermarkHud extends HudModule {

  public WatermarkHud() {
    super("Watermark", new String[] {"watermark", "wm", "water"}, Corner.TOP_LEFT);
    setDescription("Displays the client name and version.");
    this.offerProperties();
  }

  private String versionText() {
    String dirty = Exeter.DIRTY ? " (dirty)" : "";
    return String.format(" %s.%s%s", Exeter.BUILD, Exeter.HASH, dirty);
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
