package me.larp.client.module.impl.toggle.render;

import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.Property;

public final class ClickGui extends ToggleableModule {
  public enum GearMode {
    IMAGE,
    TEXT
  }

  public final Property<Boolean> showBackground = new Property<Boolean>(true, "Background", "bg");
  public final Property<Boolean> showDescriptions =
      new Property<Boolean>(true, "Descriptions", "desc");
  public final Property<Boolean> showArrow = new Property<Boolean>(true, "Arrow", "arrow");
  public final Property<Boolean> showModuleCount =
      new Property<Boolean>(false, "Module Count", "modcount");
  public final Property<Boolean> showGradient = new Property<Boolean>(true, "Gradient", "gradient");
  public final Property<Boolean> showGear =
      new Property<Boolean>(true, "Gear", "gear")
          .addChild(new EnumProperty<GearMode>(GearMode.IMAGE, "Mode", "mode"));

  public ClickGui() {
    super("ClickGui", new String[] {"clickgui"}, ModuleType.RENDER);
    setDescription("Opens the module configuration panel.");
    offerProperties(
        showBackground, showDescriptions, showGear, showArrow, showModuleCount, showGradient);
  }

  public GearMode getGearMode() {
    if (!showGear.getValue()) return GearMode.IMAGE;
    Property<?> modeProp = showGear.getChildren().get(0);
    if (modeProp instanceof EnumProperty) {
      return (GearMode) modeProp.getValue();
    }
    return GearMode.IMAGE;
  }

  public void onArrowToggled() {
    if (showArrow.getValue()) {
      showModuleCount.setValue(false);
    }
  }

  public void onModuleCountToggled() {
    if (showModuleCount.getValue()) {
      showArrow.setValue(false);
    }
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    this.minecraft.gui.setScreen(
        me.larp.client.module.impl.toggle.render.clickgui.ClickGui.getClickGui());
    this.setRunning(false);
  }
}
