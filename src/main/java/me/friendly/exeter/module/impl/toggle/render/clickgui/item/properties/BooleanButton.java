package me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Button;
import me.friendly.exeter.properties.Property;

public class BooleanButton extends Button {
  private final Property<Boolean> property;

  public BooleanButton(Property<Boolean> property) {
    super(property.getAliases()[0]);
    this.property = property;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    boolean on = getState();
    boolean hovered = isHovering(mouseX, mouseY);
    int bg = on ? (hovered ? 0xFFAA3333 : 0xFF771111) : (hovered ? 0xFF444444 : 0xFF222222);
    fill((int) this.x, (int) this.y, (int) (this.x + this.width), (int) (this.y + this.height), bg);
    FontUtil.drawString(
        this.getLabel() + ": " + (on ? "ON" : "OFF"),
        this.x + 2.0f,
        this.y + 2.0f,
        on ? -1 : -5592406);
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (mouseButton == 0 && isHovering(mouseX, mouseY)) {
      toggle();
    }
  }

  @Override
  public void toggle() {
    this.property.setValue(Boolean.valueOf(!this.property.getValue().booleanValue()));
  }

  @Override
  public boolean getState() {
    return this.property.getValue().booleanValue();
  }
}
