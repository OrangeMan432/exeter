package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;

public final class SpeedHud extends HudModule {

  public SpeedHud() {
    super("Speed", new String[] {"speedhud", "speed"}, Corner.BOTTOM_RIGHT);
    setDescription("Displays your current movement speed.");
    this.offerProperties();
  }

  @Override
  public int getWidth() {
    return FontUtil.getStringWidth("\u00a77Speed: \u00a7f0.000");
  }

  @Override
  public int getHeight() {
    return 9;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    if (minecraft.player == null) return;
    double dx = minecraft.player.getX() - minecraft.player.xOld;
    double dz = minecraft.player.getZ() - minecraft.player.zOld;
    double bps = Math.sqrt(dx * dx + dz * dz) * 20.0;
    String text = String.format("\u00a77Speed: \u00a7f%.3f", bps);
    FontUtil.drawString(text, getX(), getY(), -1);
  }
}
