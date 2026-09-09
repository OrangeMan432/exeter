package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.Property;

public final class ClickGui extends ToggleableModule {
  public enum GearMode {
    IMAGE,
    TEXT
  }

  public enum PanelAlignment {
    CENTERED,
    TOP_LEFT
  }

  public enum DescriptionMode {
    HOVER,
    PANEL
  }

  public final Property<Boolean> showBackground = new Property<Boolean>(true, "Background", "bg");
  public final Property<Boolean> showDescriptions =
      new Property<Boolean>(true, "Descriptions", "desc")
          .addChild(new EnumProperty<DescriptionMode>(DescriptionMode.HOVER, "Mode", "mode"));
  public final Property<Boolean> showArrow = new Property<Boolean>(true, "Arrow", "arrow");
  public final Property<Boolean> showModuleCount =
      new Property<Boolean>(false, "Module Count", "modcount");
  public final Property<Boolean> showGradient = new Property<Boolean>(true, "Gradient", "gradient");
  public final Property<Boolean> showGear =
      new Property<Boolean>(true, "Gear", "gear")
          .addChild(new EnumProperty<GearMode>(GearMode.IMAGE, "Mode", "mode"));
  public final EnumProperty<PanelAlignment> panelAlignment =
      new EnumProperty<PanelAlignment>(PanelAlignment.CENTERED, "Panel Alignment", "alignment");

  public ClickGui() {
    super("ClickGui", new String[] {"clickgui"}, ModuleType.RENDER);
    setDescription("Opens the module configuration panel.");
    offerProperties(
        showBackground, showDescriptions, showGear, showArrow, showModuleCount, showGradient,
        panelAlignment);
  }

  public GearMode getGearMode() {
    if (!showGear.getValue()) return GearMode.IMAGE;
    Property<?> modeProp = showGear.getChildren().get(0);
    if (modeProp instanceof EnumProperty) {
      return (GearMode) modeProp.getValue();
    }
    return GearMode.IMAGE;
  }

  public DescriptionMode getDescriptionMode() {
    if (!showDescriptions.getValue()) return null;
    Property<?> modeProp = showDescriptions.getChildren().get(0);
    if (modeProp instanceof EnumProperty) {
      return (DescriptionMode) modeProp.getValue();
    }
    return DescriptionMode.HOVER;
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
        me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui.getClickGui());
    this.setRunning(false);
  }
}
