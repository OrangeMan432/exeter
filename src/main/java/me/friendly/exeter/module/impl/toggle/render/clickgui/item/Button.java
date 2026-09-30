package me.friendly.exeter.module.impl.toggle.render.clickgui.item;

import me.friendly.api.interfaces.Labeled;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.active.render.Colors;

public class Button extends Item implements Labeled {
  private boolean state;

  public Button(String label) {
    super(label);
    this.height = 15;
  }

  protected boolean useGradient() {
    if (me.friendly.exeter.core.Exeter.getInstance() == null) return true;
    me.friendly.exeter.module.Module module =
        me.friendly.exeter.core.Exeter.getInstance().getModuleManager().getModuleByAlias("clickgui");
    if (module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui) {
      return ((me.friendly.exeter.module.impl.toggle.render.ClickGui) module)
          .showGradient.getValue().booleanValue();
    }
    return true;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    int topColor;
    int bottomColor;
    if (this.getState()) {
      if (!this.isHovering(mouseX, mouseY)) {
        topColor = Colors.getClientColorCustomAlpha(88);
        bottomColor = Colors.getClientColorCustomAlpha(55);
      } else {
        topColor = Colors.getClientColorCustomAlpha(44);
        bottomColor = Colors.getClientColorCustomAlpha(77);
      }
    } else if (!this.isHovering(mouseX, mouseY)) {
      topColor = Colors.getDarkerClientColorCustomAlpha(77);
      bottomColor = Colors.getDarkerClientColorCustomAlpha(55);
    } else {
      topColor = Colors.getDarkerClientColorCustomAlpha(33);
      bottomColor = Colors.getDarkerClientColorCustomAlpha(66);
    }

    if (useGradient()) {
      fillGradient(
          (int) this.x, (int) this.y, (int) (this.x + this.width), (int) (this.y + this.height),
          topColor, bottomColor);
    } else {
      fill((int) this.x, (int) this.y, (int) (this.x + this.width), (int) (this.y + this.height),
          topColor);
    }

    FontUtil.drawString(
        this.getLabel(), this.x + 2.0f, this.y + 4.0f, this.getState() ? -1 : -5592406);
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
    return 15;
  }
}
