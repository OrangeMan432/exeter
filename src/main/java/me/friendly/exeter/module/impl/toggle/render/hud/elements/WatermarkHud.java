package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;

public final class WatermarkHud extends HudModule {

  public WatermarkHud() {
    super("Watermark", new String[] {"watermark", "wm"}, Corner.TOP_LEFT);
    offerProperties();
  }

  @Override
  public int getWidth() {
    return FontUtil.getStringWidth("Exeter b1.7.3");
  }

  @Override
  public int getHeight() {
    return FontUtil.getFontHeight();
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    FontUtil.drawString("Exeter b1.7.3", (float) getX(), (float) getY(), 0xFFFFFFFF);
  }
}
