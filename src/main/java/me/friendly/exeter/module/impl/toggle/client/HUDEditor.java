package me.friendly.exeter.module.impl.toggle.client;

import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.hud.HudEditorScreen;
import me.friendly.exeter.properties.NumberProperty;

public final class HUDEditor extends ToggleableModule {
  private final NumberProperty<Integer> snapRange =
      new NumberProperty<Integer>(48, 0, 128, "Snap Range");

  public HUDEditor() {
    super("HUDEditor", new String[] {"hudeditor", "hudedit"}, ModuleType.CLIENT);
    setDescription("Opens the HUD editor to reposition overlay elements.");
    offerProperties(snapRange);
    snapRange.setDescription("Controls how closely HUD elements snap to edges and each other.");
  }

  /** Snap radius in pixels; 0 disables corner snapping (everything drops free). */
  public static int snapRange() {
    if (Exeter.getInstance() == null) return 48;
    var module = Exeter.getInstance().getModuleManager().getModuleByAlias("hudeditor");
    if (module instanceof HUDEditor editor) {
      return editor.snapRange.getValue();
    }
    return 48;
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    this.minecraft.gui.setScreen(HudEditorScreen.getInstance());
    this.setRunning(false);
  }
}
