package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;

public final class CoordsHud extends HudModule {

  public CoordsHud() {
    super("Coords", new String[] {"coords", "coord", "c", "cord"}, Corner.BOTTOM_RIGHT);
    setDescription("Displays your current coordinates.");
    this.offerProperties();
  }

  @Override
  public int getWidth() {
    String text = String.format("\u00a77XYZ: \u00a7f%s, %s, %s", 0, 0, 0);
    return FontUtil.getStringWidth(text);
  }

  @Override
  public int getHeight() {
    return 9;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    String text =
        String.format(
            "\u00a77XYZ: \u00a7f%s, %s, %s",
            (int) minecraft.player.getX(),
            (int) minecraft.player.getY(),
            (int) minecraft.player.getZ());
    FontUtil.drawString(text, getX(), getY(), -1);
  }
}
