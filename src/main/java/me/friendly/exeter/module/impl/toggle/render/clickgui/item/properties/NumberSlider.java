package me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties;

import com.mojang.blaze3d.platform.InputConstants;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.clickgui.ClickGui;
import me.friendly.exeter.module.impl.toggle.render.clickgui.Panel;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Item;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.PropertyItem;
import me.friendly.exeter.properties.NumberProperty;

public class NumberSlider extends Item implements PropertyItem {
  private NumberProperty numberProperty;
  private Number min;
  private Number max;
  private int difference;
  private final boolean child;
  private boolean dragging;
  private static final float CHILD_OFFSET = 1.0f;

  public NumberSlider(NumberProperty numberProperty, boolean child) {
    super(numberProperty.getAliases()[0]);
    this.numberProperty = numberProperty;
    this.min = (Number) numberProperty.getMinimum();
    this.max = (Number) numberProperty.getMaximum();
    this.difference = max.intValue() - min.intValue();
    this.child = child;
  }

  @Override
  public boolean isVisible() {
    return numberProperty == null || numberProperty.isVisible();
  }

  @Override
  public NumberProperty<?> getProperty() {
    return this.numberProperty;
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    dragSetting(mouseX, mouseY);
    float offsetX = child ? CHILD_OFFSET : 0.0f;
    float h = (float) this.getHeight();
    int fillColor;
    var guiMod =
        me.friendly.exeter.core.Exeter.getInstance()
            .getModuleManager()
            .getModuleByAlias("clickgui");
    me.friendly.exeter.module.impl.toggle.render.ClickGui cg =
        guiMod instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui c ? c : null;
    boolean rolling = cg != null && cg.rollingRainbow.getValue();
    boolean horizontal =
        rolling
            && cg.rollingDirection.getValue()
                == me.friendly.exeter.module.impl.toggle.render.ClickGui.RollingDirection
                    .HORIZONTAL;
    boolean inverse = rolling && cg.rollingInverse.getValue();
    if (rolling) {
      int screenW = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
      int screenH = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
      boolean active = !isHovering(mouseX, mouseY) && !dragging;
      fillColor =
          Colors.rollingSample(
              horizontal,
              inverse,
              (int) (x + offsetX),
              (int) y,
              screenW,
              screenH,
              active ? 77 : 55);
    } else {
      fillColor =
          !isHovering(mouseX, mouseY) && !dragging
              ? Colors.getClientColorCustomAlpha(77)
              : Colors.getClientColorCustomAlpha(55);
    }
    float fillEnd =
        ((Number) numberProperty.getValue()).floatValue() <= min.floatValue()
            ? x + offsetX
            : x + offsetX + ((float) this.width + 7.4F) * partialMultiplier();
    if (horizontal) {
      int screenW = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
      int screenH = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
      RenderMethods.drawHorizontalSpectrumRect(
          x + offsetX,
          y + h - 1.0f,
          fillEnd,
          y + h,
          sx -> Colors.rollingSample(true, inverse, sx, (int) y, screenW, screenH, 77));
    } else {
      RenderMethods.drawRect(x + offsetX, y + h - 1.0f, fillEnd, y + h, fillColor);
    }
    FontUtil.drawString(
        String.format("%s\u00a77 %s", this.getLabel(), this.numberProperty.getValue()),
        this.x + offsetX + 2.0f,
        this.y + 4.0f,
        -1);
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    if (isHovering(mouseX, mouseY) && mouseButton == InputConstants.MOUSE_BUTTON_LEFT) {
      setSettingFromX(mouseX);
      dragging = true;
    }
  }

  @Override
  public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
    super.mouseReleased(mouseX, mouseY, releaseButton);
    if (releaseButton == InputConstants.MOUSE_BUTTON_LEFT) {
      dragging = false;
    }
  }

  private void setSettingFromX(int mouseX) {
    float percent = Math.max(0, Math.min(1, (mouseX - x) / (width + 7.4F)));
    if (numberProperty.getValue() instanceof Double) {
      double result = (Double) numberProperty.getMinimum() + (difference * percent);
      numberProperty.setValue(Math.round(10.0 * result) / 10.0);
    } else if (numberProperty.getValue() instanceof Float) {
      float result = (Float) numberProperty.getMinimum() + (difference * percent);
      numberProperty.setValue(Math.round(10.0f * result) / 10.0f);
    } else if (numberProperty.getValue() instanceof Integer) {
      numberProperty.setValue(
          ((Integer) numberProperty.getMinimum() + (int) (difference * percent)));
    }
  }

  @Override
  public int getHeight() {
    return 15;
  }

  private void dragSetting(int mouseX, int mouseY) {
    if (dragging) {
      setSettingFromX(mouseX);
    }
  }

  private boolean isHovering(int mouseX, int mouseY) {
    for (Panel panel : ClickGui.getClickGui().getPanels()) {
      if (!panel.drag) continue;
      return false;
    }
    return (float) mouseX >= this.getX()
        && (float) mouseX <= this.getX() + (float) this.getWidth()
        && (float) mouseY >= this.getY()
        && (float) mouseY <= this.getY() + (float) this.getHeight();
  }

  private float middle() {
    return max.floatValue() - min.floatValue();
  }

  private float part() {
    return ((Number) numberProperty.getValue()).floatValue() - min.floatValue();
  }

  private float partialMultiplier() {
    return part() / middle();
  }
}
