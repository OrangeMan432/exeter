package me.friendly.exeter.module.impl.toggle.render.hud.elements;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.module.impl.toggle.render.hud.HudEditorScreen;
import me.friendly.exeter.module.impl.toggle.render.hud.HudModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public final class ArmorHud extends HudModule {

  private enum Orientation {
    HORIZONTAL,
    VERTICAL
  }

  private final EnumProperty<Orientation> orientation =
      new EnumProperty<Orientation>(Orientation.HORIZONTAL, "Orientation", "o");
  private final Property<Boolean> showDurability = new Property<Boolean>(false, "Durability", "dur");

  private final ItemRenderer itemRenderer = new ItemRenderer();

  public ArmorHud() {
    super("Armor", new String[] {"armor", "a"}, Corner.BOTTOM_LEFT);
    setDescription("Displays your equipped armor.");
    offerProperties(orientation, showDurability);
  }

  private ItemStack[] armor() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null || mc.player.inventory == null) {
      return new ItemStack[0];
    }
    ItemStack[] armor = mc.player.inventory.armor;
    if (equippedCount(armor) == 0 && mc.currentScreen instanceof HudEditorScreen) {
      // Editor preview when naked: full diamond for positioning.
      return new ItemStack[] {
        new ItemStack(313, 1, 0),
        new ItemStack(312, 1, 0),
        new ItemStack(311, 1, 0),
        new ItemStack(310, 1, 0)
      };
    }
    return armor;
  }

  private int equippedCount() {
    return equippedCount(armor());
  }

  private static int equippedCount(ItemStack[] armor) {
    int count = 0;
    for (ItemStack stack : armor) {
      if (stack != null && stack.count > 0) count++;
    }
    return count;
  }

  private static String durabilityText(ItemStack stack) {
    int remaining = stack.getMaxDamage() - stack.getDamage();
    float percentage = (float) remaining / stack.getMaxDamage() * 100.0F;
    return String.format("%.1f%%", Float.valueOf(percentage));
  }

  private static int durabilityColor(ItemStack stack) {
    int remaining = stack.getMaxDamage() - stack.getDamage();
    float percentage = (float) remaining / stack.getMaxDamage() * 100.0F;
    int bucket = Math.round(percentage / (100.0F / 6));
    switch (bucket) {
      case 0:
        return 0xFFFF4444;
      case 1:
        return 0xFFCC6644;
      case 2:
        return 0xFFCC9944;
      case 3:
        return 0xFFCCCC44;
      case 4:
        return 0xFF44CC44;
      default:
        return 0xFF22AA22;
    }
  }

  @Override
  public int getWidth() {
    if (orientation.getValue() == Orientation.VERTICAL) {
      int maxText = 0;
      for (ItemStack stack : armor()) {
        if (stack == null || stack.count <= 0) continue;
        if (showDurability.getValue().booleanValue() && stack.isDamageable()) {
          int w = FontUtil.getStringWidth(durabilityText(stack));
          if (w > maxText) maxText = w;
        }
      }
      if (equippedCount() == 0) return 0;
      return 18 + maxText + 4;
    }
    return 72;
  }

  @Override
  public int getHeight() {
    int count = equippedCount();
    if (count == 0) return 0;
    if (orientation.getValue() == Orientation.VERTICAL) {
      return count * 18;
    }
    return 18;
  }

  @Override
  public void render(int scaledWidth, int scaledHeight) {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null) return;
    ItemStack[] armor = armor();
    // Item rendering touches lighting/texture/blend state that the HUD text
    // renderer depends on; isolate it so the rest of the HUD keeps rendering.
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      renderItems(mc, armor);
    } finally {
      GL11.glPopAttrib();
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
    if (orientation.getValue() == Orientation.VERTICAL
        && showDurability.getValue().booleanValue()) {
      renderDurabilityText(armor);
    }
  }

  private void renderItems(Minecraft mc, ItemStack[] armor) {
    if (orientation.getValue() == Orientation.VERTICAL) {
      int x = getX();
      int y = getY();
      // armor[3] is helmet down to armor[0] boots.
      for (int i = 3; i >= 0; --i) {
        if (i >= armor.length) continue;
        ItemStack stack = armor[i];
        if (stack == null || stack.count <= 0) continue;
        itemRenderer.method_1487(mc.textRenderer, mc.textureManager, stack, x, y);
        itemRenderer.method_1488(mc.textRenderer, mc.textureManager, stack, x, y);
        y += 18;
      }
    } else {
      int x = getX();
      int y = getY();
      for (int i = 3; i >= 0; --i) {
        if (i >= armor.length) continue;
        ItemStack stack = armor[i];
        if (stack == null || stack.count <= 0) continue;
        itemRenderer.method_1487(mc.textRenderer, mc.textureManager, stack, x, y);
        itemRenderer.method_1488(mc.textRenderer, mc.textureManager, stack, x, y);
        x += 18;
      }
    }
  }

  private void renderDurabilityText(ItemStack[] armor) {
    int x = getX();
    int y = getY();
    for (int i = 3; i >= 0; --i) {
      if (i >= armor.length) continue;
      ItemStack stack = armor[i];
      if (stack == null || stack.count <= 0) continue;
      if (stack.isDamageable()) {
        FontUtil.drawString(
            durabilityText(stack), (float) (x + 18), (float) (y + 5), durabilityColor(stack));
      }
      y += 18;
    }
  }
}
