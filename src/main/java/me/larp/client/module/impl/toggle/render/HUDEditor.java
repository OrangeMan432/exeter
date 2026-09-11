package me.larp.client.module.impl.toggle.render;

import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.module.impl.toggle.render.hud.HudEditorScreen;

public final class HUDEditor extends ToggleableModule {
  public HUDEditor() {
    super("HUDEditor", new String[] {"hudeditor", "hudedit"}, ModuleType.RENDER);
    setDescription("Opens the HUD editor to reposition overlay elements.");
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    this.minecraft.gui.setScreen(HudEditorScreen.getInstance());
    this.setRunning(false);
  }
}
