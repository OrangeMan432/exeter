package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ArmorHud extends HudModule {
  private final EnumProperty<Orientation> orientation =
      new EnumProperty<Orientation>(Orientation.HORIZONTAL, "Orientation", "o");
  private final Property<Boolean> showDurability = new Property<>(false, "Durability", "dur");

  public ArmorHud() {
    super("Armor", new String[] {"armor", "a"}, Corner.BOTTOM_LEFT);
    setDescription("Displays your equipped armor.");
    this.offerProperties(orientation, showDurability);
  }

  @Override
  public int getWidth() {
    if (orientation.getValue() == Orientation.VERTICAL) {
      int maxText = 0;
      int count = 0;
      if (minecraft.player == null) return 0;
      for (int i = 3; i >= 0; --i) {
        EquipmentSlot slot = slotFromIndex(i);
        ItemStack stack = minecraft.player.getItemBySlot(slot);
        if (stack == null || stack.isEmpty()) continue;
        count++;
        if (showDurability.getValue() && stack.isDamageableItem()) {
          int w = FontUtil.getStringWidth(getDurabilityText(stack));
          if (w > maxText) maxText = w;
        }
      }
      if (count == 0) return 0;
      return 18 + maxText + 4;
    }
    return 72;
  }

  @Override
  public int getHeight() {
    int count = 0;
    if (minecraft.player != null) {
      for (int i = 3; i >= 0; --i) {
        EquipmentSlot slot = slotFromIndex(i);
        ItemStack stack = minecraft.player.getItemBySlot(slot);
        if (stack != null && !stack.isEmpty()) count++;
      }
    }
    if (count == 0) return 0;
    if (orientation.getValue() == Orientation.VERTICAL) {
      return count * 16;
    }
    return 16;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    if (minecraft.player == null) return;
    int x = getX();
    int y = getY();
    GuiGraphicsExtractor gui = RenderMethods.guiGraphics;
    if (gui == null) return;

    if (orientation.getValue() == Orientation.VERTICAL) {
      int textX = x + 18;
      for (int i = 3; i >= 0; --i) {
        EquipmentSlot slot = slotFromIndex(i);
        ItemStack stack = minecraft.player.getItemBySlot(slot);
        if (stack == null || stack.isEmpty()) continue;
        gui.item(stack, x, y);
        gui.itemDecorations(minecraft.font, stack, x, y);
        y += 16;
      }
      gui.nextStratum();
      y = getY();
      for (int i = 3; i >= 0; --i) {
        EquipmentSlot slot = slotFromIndex(i);
        ItemStack stack = minecraft.player.getItemBySlot(slot);
        if (stack == null || stack.isEmpty()) continue;
        if (showDurability.getValue() && stack.isDamageableItem()) {
          FontUtil.drawString(getDurabilityText(stack), textX, y + 5, getDurabilityColor(stack));
        }
        y += 16;
      }
    } else {
      for (int i = 3; i >= 0; --i) {
        EquipmentSlot slot = slotFromIndex(i);
        ItemStack stack = minecraft.player.getItemBySlot(slot);
        if (stack == null || stack.isEmpty()) continue;
        gui.item(stack, x, y);
        gui.itemDecorations(minecraft.font, stack, x, y);
        x += 18;
      }
    }
  }

  private static EquipmentSlot slotFromIndex(int index) {
    return switch (index) {
      case 3 -> EquipmentSlot.HEAD;
      case 2 -> EquipmentSlot.CHEST;
      case 1 -> EquipmentSlot.LEGS;
      default -> EquipmentSlot.FEET;
    };
  }

  private static String getDurabilityText(ItemStack stack) {
    int damageValue = stack.getMaxDamage() - stack.getDamageValue();
    int maxDamage = stack.getMaxDamage();
    float percentage = (float) damageValue / maxDamage * 100;
    return String.format("%.1f%%", percentage);
  }

  private static int getDurabilityColor(ItemStack stack) {
    int damageValue = stack.getMaxDamage() - stack.getDamageValue();
    int maxDamage = stack.getMaxDamage();
    float percentage = (float) damageValue / maxDamage * 100;
    int bucket = (int) Math.round(percentage / (100f / 6));
    return switch (bucket) {
      case 0 -> 0xFFFF4444;
      case 1 -> 0xFFCC6644;
      case 2 -> 0xFFCC9944;
      case 3 -> 0xFFCCCC44;
      case 4 -> 0xFF44CC44;
      default -> 0xFF22AA22;
    };
  }

  private static enum Orientation {
    HORIZONTAL,
    VERTICAL;
  }
}
