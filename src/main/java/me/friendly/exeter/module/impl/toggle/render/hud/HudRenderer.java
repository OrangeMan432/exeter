package me.friendly.exeter.module.impl.toggle.render.hud;

import java.util.List;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.module.Module;
import net.minecraft.client.Minecraft;

/** Central renderer for all HUD elements. */
public final class HudRenderer extends Module {

  public HudRenderer() {
    super("HudRenderer", new String[] {"hudrenderer"});
    setDescription("Central renderer for all HUD elements.");
  }

  public static void renderAll(int sw, int sh) {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null) {
      return;
    }
    List<HudModule> active = HudModule.getActive();
    HudModule.layoutByCorner(active, sw, sh);
    for (HudModule module : active) {
      try {
        module.render(sw, sh);
      } catch (Exception ignored) {
      }
    }
  }
}
