package me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Button;
import me.friendly.exeter.properties.PopupProperty;

public class PopupButton extends Button {
  private final PopupProperty property;

  public PopupButton(PopupProperty property) {
    super(property.getAliases()[0]);
    this.property = property;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    boolean hovered = isHovering(mouseX, mouseY);
    fill((int) this.x, (int) this.y, (int) (this.x + this.width), (int) (this.y + this.height),
        hovered ? 0xFF444444 : 0xFF222222);
    FontUtil.drawString(this.getLabel(), this.x + 2.0f, this.y + 2.0f, -1);
    FontUtil.drawString(">", this.x + this.width - 8.0f, this.y + 2.0f, 0xFFCCCCCC);
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (mouseButton == 0 && isHovering(mouseX, mouseY)) {
      property.getOpenAction().run();
    }
  }

  @Override
  public boolean getState() {
    return false;
  }
}
