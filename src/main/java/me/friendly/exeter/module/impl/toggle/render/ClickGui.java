package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGuiScreen;
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

  private ClickGuiScreen screen;

  public ClickGui() {
    super("ClickGui", new String[] {"clickgui", "gui"}, 0xFFAA55, ModuleType.RENDER);
    setDescription("Opens the module configuration panel.");
    offerProperties(
        showBackground, showDescriptions, showArrow, showGradient, searchEnabled, showBorder);
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
