package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import net.minecraft.client.Minecraft;

public final class DirectionHud extends HudModule {

  public DirectionHud() {
    super("Direction", new String[] {"direction", "facing", "d"}, Corner.BOTTOM_RIGHT);
    setDescription("Displays the direction you are facing.");
    offerProperties();
  }

  private String facing() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null) {
      return "SOUTH";
    }
    int index = (int) Math.floor(mc.player.yaw * 4.0 / 360.0 + 0.5) & 3;
    switch (index) {
      case 0:
        return "SOUTH";
      case 1:
        return "WEST";
      case 2:
        return "NORTH";
      default:
        return "EAST";
    }
  }

  @Override
  public int getWidth() {
    return FontUtil.getStringWidth(facing());
  }

  @Override
  public int getHeight() {
    return FontUtil.getFontHeight();
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    FontUtil.drawString(
        "\u00a77" + facing(), (float) getX(), (float) getY(), 0xFFFFFFFF);
  }
}
