package me.larp.client.module.impl.toggle.render.clickgui.item.properties;

import me.larp.api.minecraft.render.RenderMethods;
import me.larp.api.minecraft.render.font.FontUtil;
import me.larp.client.module.impl.active.render.Colors;
import me.larp.client.module.impl.toggle.render.clickgui.item.Button;
import me.larp.client.properties.EnumProperty;

public class EnumButton extends Button {
  private EnumProperty property;
  private final boolean child;
  private static final float CHILD_OFFSET = 1.0f;

  public EnumButton(EnumProperty property, boolean child) {
    super(property.getAliases()[0]);
    this.property = property;
    this.child = child;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    float offsetX = child ? CHILD_OFFSET : 0.0f;
    RenderMethods.drawRect(
        this.x + offsetX,
        this.y,
        this.x + offsetX + (float) this.width + 7.4f,
        this.y + (float) this.height,
        this.getState()
            ? (!this.isHovering(mouseX, mouseY)
                ? Colors.getClientColorCustomAlpha(77)
                : Colors.getClientColorCustomAlpha(55))
            : (!this.isHovering(mouseX, mouseY) ? 0x11333333 : -2009910477));
    FontUtil.drawString(
        String.format("%s\u00a77 %s", this.getLabel(), this.property.getFixedValue()),
        this.x + offsetX + 2.0f,
        this.y + 4.0f,
        this.getState() ? -1 : -5592406);
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    if (this.isHovering(mouseX, mouseY)) {
      if (mouseButton == 0) {
        this.property.increment();
      } else if (mouseButton == 1) {
        this.property.decrement();
      }
    }
  }

  @Override
  public int getHeight() {
    return 15;
  }

  @Override
  public void toggle() {}

  @Override
  public boolean getState() {
    return true;
  }
}
