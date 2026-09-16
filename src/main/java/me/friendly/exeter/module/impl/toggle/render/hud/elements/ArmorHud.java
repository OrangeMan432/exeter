package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ArmorHud extends HudModule {

  public ArmorHud() {
    super("Armor", new String[] {"armor", "a"}, Corner.BOTTOM_LEFT);
    setDescription("Displays your equipped armor.");
    this.offerProperties();
  }

  @Override
  public int getWidth() {
    return 72;
  }

  @Override
  public int getHeight() {
    return 16;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    int x = getX();
    int y = getY();
    GuiGraphicsExtractor gui = RenderMethods.guiGraphics;
    if (gui == null || minecraft.player == null) return;
    for (int index = 3; index >= 0; --index) {
      EquipmentSlot slot =
          index == 3
              ? EquipmentSlot.HEAD
              : (index == 2
                  ? EquipmentSlot.CHEST
                  : (index == 1 ? EquipmentSlot.LEGS : EquipmentSlot.FEET));
      ItemStack stack = minecraft.player.getItemBySlot(slot);
      if (stack == null || stack.isEmpty()) continue;
      gui.item(stack, x, y);
      gui.itemDecorations(minecraft.font, stack, x, y);
      x += 18;
    }
  }
}
