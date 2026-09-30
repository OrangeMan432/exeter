package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;

public final class WatermarkHud extends HudModule {

  public WatermarkHud() {
    super("Watermark", new String[] {"watermark", "wm", "water"}, Corner.TOP_LEFT);
    setDescription("Displays the client name and version.");
    offerProperties();
  }

  private String text() {
    String dirty = Exeter.DIRTY ? " (dirty)" : "";
    return String.format("%s \u00a77%s.%s%s", Exeter.TITLE, Exeter.BUILD, Exeter.HASH, dirty);
  }

  @Override
  public int getWidth() {
    return FontUtil.getStringWidth(text());
  }

  @Override
  public int getHeight() {
    return FontUtil.getFontHeight();
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    FontUtil.drawString(text(), (float) getX(), (float) getY(), 0xFFFFFFFF);
  }
}
