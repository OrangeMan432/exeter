package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import net.minecraft.client.Minecraft;

public final class FpsHud extends HudModule {

  public FpsHud() {
    super("Fps", new String[] {"fps", "frames"}, Corner.TOP_LEFT);
    setDescription("Displays your current framerate.");
    this.offerProperties();
  }

  @Override
  public int getWidth() {
    return FontUtil.getStringWidth("\u00a77FPS: \u00a7f000");
  }

  @Override
  public int getHeight() {
    return 9;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    String text = String.format("\u00a77FPS: \u00a7f%s", Minecraft.getInstance().getFps());
    FontUtil.drawString(text, getX(), getY(), -1);
  }
}
