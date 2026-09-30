package me.friendly.exeter.module.impl.toggle.render.clickgui.item;

import me.friendly.api.interfaces.Labeled;
import me.friendly.api.minecraft.render.font.FontUtil;

public class Button extends Item implements Labeled {
  private boolean state;

  public Button(String label) {
    super(label);
    this.height = 12;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    boolean active = getState();
    boolean hovered = isHovering(mouseX, mouseY);
    int bg;
    if (active) {
      bg = hovered ? 0xFFAA3333 : 0xFF771111;
    } else {
      bg = hovered ? 0xFF444444 : 0xFF222222;
    }
    fill((int) this.x, (int) this.y, (int) (this.x + this.width), (int) (this.y + this.height), bg);
    FontUtil.drawString(this.getLabel(), this.x + 2.0f, this.y + 2.0f, active ? -1 : -5592406);
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
      this.state = !this.state;
      this.toggle();
    }
  }

  public void toggle() {}

  public boolean getState() {
    return this.state;
  }

  @Override
  public int getHeight() {
    return 12;
  }
}
