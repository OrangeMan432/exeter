package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.hud.HudEditorScreen;
import net.minecraft.client.Minecraft;

public final class HUDEditor extends ToggleableModule {
  public HUDEditor() {
    super("HUDEditor", new String[] {"hudeditor", "hudedit"}, ModuleType.CLIENT);
    setDescription("Opens the HUD editor to reposition overlay elements.");
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc != null) {
      mc.setScreen(HudEditorScreen.getInstance());
    }
    setRunning(false);
  }
}
