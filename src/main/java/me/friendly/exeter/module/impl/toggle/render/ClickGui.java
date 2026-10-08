package me.friendly.exeter.module.impl.toggle.render;

import java.util.HashMap;
import java.util.Map;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.ActionProperty;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
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

  public enum RollingDirection {
    VERTICAL,
    HORIZONTAL
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
  public final Property<Boolean> searchEnabled = new Property<Boolean>(true, "Search", "search");
  public final Property<Boolean> showBorder = new Property<Boolean>(false, "Border", "border");
  public final Property<Boolean> rollingRainbow =
      new Property<Boolean>(false, "Rolling Rainbow", "rollingrainbow");
  public final EnumProperty<RollingDirection> rollingDirection =
      new EnumProperty<RollingDirection>(
          RollingDirection.VERTICAL, "Rolling Direction", "rollingdirection");
  public final Property<Boolean> rollingInverse =
      new Property<Boolean>(false, "Rolling Inverse", "rollinginverse");
  private final NumberProperty<Integer> stripWidth =
      new NumberProperty<Integer>(4, 1, 8, "Strip Width", "stripwidth");
  public final ActionProperty resetPositions =
      new ActionProperty("Reset Positions", this::resetPositions);

  private final Map<String, int[]> pendingPanels = new HashMap<>();

  public ClickGui() {
    super("ClickGui", new String[] {"clickgui"}, ModuleType.RENDER);
    setDescription("Opens the module configuration panel.");
    rollingDirection.visibleWhen(() -> rollingRainbow.getValue());
    rollingInverse.visibleWhen(() -> rollingRainbow.getValue());
    stripWidth.visibleWhen(
        () ->
            rollingRainbow.getValue()
                && rollingDirection.getValue() == RollingDirection.HORIZONTAL);
    stripWidth.visibleWhen(
        () ->
            rollingRainbow.getValue()
                && rollingDirection.getValue() == RollingDirection.HORIZONTAL);
    offerProperties(
        showBackground,
        showDescriptions,
        showGear,
        showArrow,
        showModuleCount,
        showGradient,
        panelAlignment,
        searchEnabled,
        showBorder,
        rollingRainbow,
        rollingDirection,
        stripWidth,
        rollingInverse,
        resetPositions);
  }

  /** Spectrum strip width for horizontal gradients; falls back to 4px. */
  public static float spectrumStripWidth() {
    try {
      var module =
          me.friendly.exeter.core.Exeter.getInstance()
              .getModuleManager()
              .getModuleByAlias("clickgui");
      if (module instanceof ClickGui cg) {
        return cg.stripWidth.getValue().floatValue();
      }
    } catch (Exception ignored) {
    }
    return 4.0f;
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

  public void onAlignmentChanged() {
    resetPositions();
  }

  /** Panel positions loaded from clickgui.toml, applied when the screen opens. */
  public void setPendingPanels(Map<String, int[]> positions) {
    pendingPanels.clear();
    pendingPanels.putAll(positions);
  }

  public Map<String, int[]> getPendingPanels() {
    return pendingPanels;
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    this.minecraft.gui.setScreen(
        me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui.getClickGui());
    this.setRunning(false);
  }

  private void resetPositions() {
    me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui screen =
        me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui.getClickGui();
    screen.resetPanels();
    me.friendly.exeter.util.NotificationManager.push("Panel positions reset", "warning");
  }
}
