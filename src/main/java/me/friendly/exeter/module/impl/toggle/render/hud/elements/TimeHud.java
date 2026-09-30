package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import java.text.SimpleDateFormat;
import java.util.Date;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;

public final class TimeHud extends HudModule {

  private final SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a");

  public TimeHud() {
    super("Time", new String[] {"time", "t"}, Corner.BOTTOM_RIGHT);
    setDescription("Displays the current system time.");
    offerProperties();
  }

  @Override
  public int getWidth() {
    return FontUtil.getStringWidth(dateFormat.format(new Date()));
  }

  @Override
  public int getHeight() {
    return FontUtil.getFontHeight();
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    FontUtil.drawString(
        "\u00a77" + dateFormat.format(new Date()), (float) getX(), (float) getY(), 0xFFFFFFFF);
  }
}
