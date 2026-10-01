package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.helper.PlayerHelper;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;

public final class DirectionHud extends HudModule {

  public DirectionHud() {
    super("Direction", new String[] {"direction", "facing", "d"}, Corner.BOTTOM_RIGHT);
    setDescription("Displays the direction you are facing.");
    this.offerProperties();
  }

  @Override
  public int getWidth() {
    return FontUtil.getStringWidth(PlayerHelper.getFacingWithProperCapitals().toUpperCase());
  }

  @Override
  public int getHeight() {
    return 9;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    String text = PlayerHelper.getFacingWithProperCapitals().toUpperCase();
    FontUtil.drawString(text, getX(), getY(), Colors.getHudMain());
  }
}
