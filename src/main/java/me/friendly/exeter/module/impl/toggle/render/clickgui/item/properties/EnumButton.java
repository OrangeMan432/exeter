package me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Button;
import me.friendly.exeter.properties.EnumProperty;

public class EnumButton extends Button {
  private final EnumProperty<?> property;

  public EnumButton(EnumProperty<?> property) {
    super(property.getAliases()[0]);
    this.property = property;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    boolean hovered = isHovering(mouseX, mouseY);
    fill((int) this.x, (int) this.y, (int) (this.x + this.width) + 7,
        (int) (this.y + this.height), hovered ? 0xFF444444 : 0xFF222222);
    FontUtil.drawString(
        this.getLabel() + ": " + property.getFixedValue(),
        this.x + 2.0f,
        this.y + 4.0f,
        -1);
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (isHovering(mouseX, mouseY)) {
      if (mouseButton == 0) {
        property.increment();
      } else if (mouseButton == 1) {
        property.decrement();
      }
    }
  }

  @Override
  public boolean getState() {
    return false;
  }
}
