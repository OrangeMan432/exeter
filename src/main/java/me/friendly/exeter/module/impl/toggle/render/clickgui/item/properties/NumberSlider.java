package me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties;

import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.active.render.Colors;
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
      FontUtil.drawString(
          this.getLabel() + ": " + property.getValue(), this.x + 2.0f, this.y + 4.0f, -1);
      return;
    }
    double min = property.getMinimum().doubleValue();
    double max = property.getMaximum().doubleValue();
    double value = property.getValue().doubleValue();
    double fraction = max > min ? (value - min) / (max - min) : 0.0;
    if (fraction < 0.0) fraction = 0.0;
    if (fraction > 1.0) fraction = 1.0;
    float h = (float) this.getHeight();
    int barX = (int) this.x;
    int barEnd =
        value <= min ? (int) this.x : (int) (this.x + this.width * fraction);
    fill(barX, (int) (this.y + h - 1.0f), barEnd, (int) (this.y + h),
        !isHovering(mouseX, mouseY) && !dragging
            ? Colors.getClientColorCustomAlpha(77)
            : Colors.getClientColorCustomAlpha(55));
    FontUtil.drawString(
        this.getLabel() + ": " + property.getValue(), this.x + 2.0f, this.y + 4.0f, -1);
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
    if (releaseButton == 0) {
      dragging = false;
    }
  }

  @Override
  public boolean getState() {
    return false;
  }
}
