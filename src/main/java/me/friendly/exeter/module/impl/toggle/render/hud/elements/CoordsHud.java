package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;

public final class CoordsHud extends HudModule {

  public CoordsHud() {
    super("Coords", new String[] {"coords", "coord", "c", "cord"}, Corner.BOTTOM_RIGHT);
    setDescription("Displays your current coordinates.");
    this.offerProperties();
  }

  @Override
  public int getWidth() {
    return FontUtil.getStringWidth("XYZ: ") + FontUtil.getStringWidth("0, 0, 0");
  }

  @Override
  public int getHeight() {
    return 9;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    String coords =
        String.format(
            "%s, %s, %s",
            (int) minecraft.player.getX(),
            (int) minecraft.player.getY(),
            (int) minecraft.player.getZ());
    FontUtil.drawString("XYZ: ", getX(), getY(), Colors.getHudAccent());
    FontUtil.drawString(
        coords, getX() + FontUtil.getStringWidth("XYZ: "), getY(), Colors.getHudMain());
  }
}
