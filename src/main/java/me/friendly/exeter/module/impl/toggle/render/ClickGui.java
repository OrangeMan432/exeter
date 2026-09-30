package me.friendly.exeter.module.impl.toggle.render;

import java.util.HashMap;
import java.util.Map;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.config.ExeterConfig;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGuiScreen;
import me.friendly.exeter.properties.ActionProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.Minecraft;

public final class ClickGui extends ToggleableModule {

  public final Property<Boolean> showBackground = new Property<Boolean>(true, "Background", "bg");
  public final Property<Boolean> showDescriptions =
      new Property<Boolean>(true, "Descriptions", "desc");
  public final Property<Boolean> showArrow = new Property<Boolean>(true, "Arrow", "arrow");
  public final Property<Boolean> showGradient =
      new Property<Boolean>(true, "Gradient", "gradient");
  public final Property<Boolean> searchEnabled = new Property<Boolean>(true, "Search", "search");
  public final Property<Boolean> showBorder = new Property<Boolean>(true, "Border", "border");
  public final ActionProperty resetPositions =
      new ActionProperty(
          "Reset Positions",
          new Runnable() {
            @Override
            public void run() {
              resetPositions();
            }
          });

  private final Map<String, int[]> pendingPanels = new HashMap<String, int[]>();

  public ClickGui() {
    super("ClickGui", new String[] {"clickgui", "gui"}, 0xFFAA55, ModuleType.CLIENT);
    setDescription("Opens the module configuration panel.");
    offerProperties(
        showBackground,
        showDescriptions,
        showArrow,
        showGradient,
        searchEnabled,
        showBorder,
        resetPositions);
  }

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
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc != null) {
      mc.setScreen(ClickGuiScreen.getInstance());
    }
    setRunning(false);
  }

  private void resetPositions() {
    pendingPanels.clear();
    ClickGuiScreen.getInstance().reload();
    if (ExeterConfig.getInstance() != null) {
      ExeterConfig.getInstance().saveModule(this);
    }
  }
}
