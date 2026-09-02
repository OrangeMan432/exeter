package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;

public final class ClickGui extends ToggleableModule {
  public final Property<Boolean> showBackground = new Property<Boolean>(true, "Background", "bg");
  public final Property<Boolean> showDescriptions =
      new Property<Boolean>(true, "Descriptions", "desc");
  public final Property<Boolean> showGear = new Property<Boolean>(true, "Gear", "gear");
  public final Property<Boolean> showArrow = new Property<Boolean>(true, "Arrow", "arrow");
  public final Property<Boolean> showGradient = new Property<Boolean>(true, "Gradient", "gradient");

  public ClickGui() {
    super("ClickGui", new String[] {"clickgui"}, ModuleType.RENDER);
    setDescription("Opens the module configuration panel.");
    offerProperties(showBackground, showDescriptions, showGear, showArrow, showGradient);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    this.minecraft.gui.setScreen(
        me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui.getClickGui());
    this.setRunning(false);
  }
}
