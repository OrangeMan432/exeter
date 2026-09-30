package me.friendly.exeter.module.impl.toggle.render.hud;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import net.minecraft.client.Minecraft;

public final class HudModule extends ToggleableModule {

  public HudModule() {
    super("HUD", new String[] {"hud", "overlay"}, 0x55FFFF, ModuleType.HUD);
    setDescription("Draws info text on screen.");
  }

  public static void renderHud() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null) {
      return;
    }
    int y = 4;
    FontUtil.drawString("Exeter b1.7.3", 4.0f, (float) y, 0xFFFFFFFF);
    y += 10;
    FontUtil.drawString(
        "XYZ: "
            + (int) mc.player.x
            + " / "
            + (int) mc.player.y
            + " / "
            + (int) mc.player.z,
        4.0f,
        (float) y,
        0xFFAAAAAA);
  }
}
