package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;

public final class WatermarkHud extends HudModule {

  public WatermarkHud() {
    super("Watermark", new String[] {"watermark", "wm", "water"}, Corner.TOP_LEFT);
    setDescription("Displays the client name and version.");
    this.offerProperties();
  }

  @Override
  public int getWidth() {
    String dirty = Exeter.DIRTY ? " (dirty)" : "";
    String text =
        String.format("%s \u00a77%s.%s%s", Exeter.TITLE, Exeter.BUILD, Exeter.HASH, dirty);
    return FontUtil.getStringWidth(text);
  }

  @Override
  public int getHeight() {
    return 9;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    String dirty = Exeter.DIRTY ? " (dirty)" : "";
    String text =
        String.format("%s \u00a77%s.%s%s", Exeter.TITLE, Exeter.BUILD, Exeter.HASH, dirty);
    FontUtil.drawString(text, getX(), getY(), -1);
  }
}
