package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGuiScreen;
import net.minecraft.client.Minecraft;

public final class ClickGui extends ToggleableModule {

  private ClickGuiScreen screen;

  public ClickGui() {
    super("ClickGui", new String[] {"clickgui", "gui"}, 0xFFAA55, ModuleType.RENDER);
    setDescription("Opens the module configuration panel.");
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (screen == null) {
      screen = new ClickGuiScreen();
    } else {
      screen.reload();
    }
    if (mc != null) {
      mc.setScreen(screen);
    }
    setRunning(false);
  }
}
