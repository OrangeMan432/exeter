package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import net.minecraft.client.Minecraft;

public final class CoordsHud extends HudModule {

  public CoordsHud() {
    super("Coords", new String[] {"coords", "pos"}, Corner.BOTTOM_LEFT);
    offerProperties();
  }

  private String text() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null) {
      return "XYZ: --";
    }
    return "XYZ: "
        + (int) mc.player.x
        + " / "
        + (int) mc.player.y
        + " / "
        + (int) mc.player.z;
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
    FontUtil.drawString(text(), (float) getX(), (float) getY(), 0xFFAAAAAA);
  }
}
