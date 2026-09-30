package me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Button;
import me.friendly.exeter.properties.NumberProperty;

public class NumberSlider extends Button {
  private final NumberProperty<? extends Number> property;
  private boolean dragging;

  public NumberSlider(NumberProperty<? extends Number> property) {
    super(property.getAliases()[0]);
    this.property = property;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    if (dragging) {
      applyDrag(mouseX);
    }
    if (property.getMinimum() == null || property.getMaximum() == null) {
      boolean hovered = isHovering(mouseX, mouseY);
      fill((int) this.x, (int) this.y, (int) (this.x + this.width), (int) (this.y + this.height),
          hovered ? 0xFF444444 : 0xFF222222);
      FontUtil.drawString(
          this.getLabel() + ": " + property.getValue(), this.x + 2.0f, this.y + 2.0f, -1);
      return;
    }
    double min = property.getMinimum().doubleValue();
    double max = property.getMaximum().doubleValue();
    double value = property.getValue().doubleValue();
    double fraction = max > min ? (value - min) / (max - min) : 0.0;
    if (fraction < 0.0) fraction = 0.0;
    if (fraction > 1.0) fraction = 1.0;
    boolean hovered = isHovering(mouseX, mouseY);
    fill((int) this.x, (int) this.y, (int) (this.x + this.width), (int) (this.y + this.height),
        hovered ? 0xFF333333 : 0xFF222222);
    int fillX = (int) (this.x + this.width * fraction);
    fill((int) this.x, (int) this.y, fillX, (int) (this.y + this.height), 0xFF771111);
    FontUtil.drawString(
        this.getLabel() + ": " + property.getValue(), this.x + 2.0f, this.y + 2.0f, -1);
  }

  private void applyDrag(int mouseX) {
    if (property.getMinimum() == null || property.getMaximum() == null) {
      return;
    }
    double min = property.getMinimum().doubleValue();
    double max = property.getMaximum().doubleValue();
    double fraction = (mouseX - this.x) / (double) this.width;
    if (fraction < 0.0) fraction = 0.0;
    if (fraction > 1.0) fraction = 1.0;
    setSliderValue(min + fraction * (max - min));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void setSliderValue(double value) {
    Object current = property.getValue();
    if (current instanceof Integer) {
      ((NumberProperty) property).setValue(Integer.valueOf((int) Math.round(value)));
    } else if (current instanceof Float) {
      ((NumberProperty) property).setValue(Float.valueOf((float) value));
    } else if (current instanceof Double) {
      ((NumberProperty) property).setValue(Double.valueOf(value));
    } else if (current instanceof Long) {
      ((NumberProperty) property).setValue(Long.valueOf(Math.round(value)));
    }
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (mouseButton == 0
        && isHovering(mouseX, mouseY)
        && property.getMinimum() != null
        && property.getMaximum() != null) {
      dragging = true;
      applyDrag(mouseX);
    }
  }

  @Override
  public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
    dragging = false;
  }

  @Override
  public boolean getState() {
    return false;
  }
}
