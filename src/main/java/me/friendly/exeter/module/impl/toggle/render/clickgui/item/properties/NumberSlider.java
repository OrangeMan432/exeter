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

  // Middle click typed entry: the drawn value itself becomes the edit buffer.
  private boolean editing;
  private String editText = "";
  private int caret;
  private boolean caretVisible = true;
  private long lastBlinkMs;
  private static final int MAX_CHARS = 12;

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
    String shown = editing ? editText : String.valueOf(this.numberProperty.getValue());
    float textX = this.x + offsetX + 2.0f;
    float textY = this.y + 4.0f;
    FontUtil.drawString(String.format("%s§7 %s", this.getLabel(), shown), textX, textY, -1);
    if (editing && System.currentTimeMillis() - lastBlinkMs > 500) {
      caretVisible = !caretVisible;
      lastBlinkMs = System.currentTimeMillis();
    }
    if (editing && caretVisible) {
      String before = String.format("%s§7 %s", this.getLabel(), editText.substring(0, caret));
      int caretX = (int) textX + FontUtil.getStringWidth(before);
      RenderMethods.drawRect(caretX, (int) textY, caretX + 1, (int) textY + 8, -1);
    }
  }

  /** Middle click: turn the drawn value into an edit buffer with the caret at the end. */
  public void openEditor() {
    editing = true;
    editText = formatNumber((Number) this.numberProperty.getValue());
    caret = editText.length();
    blink();
  }

  public boolean isEditing() {
    return editing;
  }

  public void cancelEditor() {
    editing = false;
    editText = "";
  }

  /** Applies the typed text, clamped to this property's range, then leaves edit mode. */
  public void commitEditor() {
    double parsed;
    try {
      parsed = Double.parseDouble(editText);
    } catch (NumberFormatException e) {
      // Nothing usable typed: keep whatever the slider already had.
      cancelEditor();
      return;
    }
    double min = ((Number) numberProperty.getMinimum()).doubleValue();
    double max = ((Number) numberProperty.getMaximum()).doubleValue();
    double clamped = Math.max(min, Math.min(max, parsed));
    Object current = numberProperty.getValue();
    if (current instanceof Double) {
      numberProperty.setValue(clamped);
    } else if (current instanceof Float) {
      numberProperty.setValue((float) clamped);
    } else if (current instanceof Long) {
      numberProperty.setValue(Math.round(clamped));
    } else {
      numberProperty.setValue((int) Math.round(clamped));
    }
    me.friendly.exeter.logging.DebugLogger.get()
        .log(
            "ClickGui",
            me.friendly.exeter.logging.DebugLogger.Level.INFO,
            this.getLabel() + " set to " + numberProperty.getValue() + " (typed " + editText + ")");
    cancelEditor();
  }

  /**
   * @return true when the key belonged to the editor.
   */
  public boolean keyPressedInEditor(int keyCode, int scanCode) {
    if (!editing) return false;
    if (keyCode == InputConstants.KEY_BACKSPACE) {
      if (caret > 0) {
        editText = editText.substring(0, caret - 1) + editText.substring(caret);
        caret--;
        blink();
      }
      return true;
    }
    if (keyCode == InputConstants.KEY_DELETE) {
      if (caret < editText.length()) {
        editText = editText.substring(0, caret) + editText.substring(caret + 1);
        blink();
      }
      return true;
    }
    if (keyCode == InputConstants.KEY_LEFT) {
      caret = Math.max(0, caret - 1);
      blink();
      return true;
    }
    if (keyCode == InputConstants.KEY_RIGHT) {
      caret = Math.min(editText.length(), caret + 1);
      blink();
      return true;
    }
    // Typed characters arrive as scan codes, same as the color picker's hex field.
    if (scanCode >= 32 && scanCode < 127) {
      char c = (char) scanCode;
      if (((c >= '0' && c <= '9') || c == '.' || c == '-') && editText.length() < MAX_CHARS) {
        editText = editText.substring(0, caret) + c + editText.substring(caret);
        caret++;
        blink();
      }
      return true;
    }
    return false;
  }

  private void blink() {
    lastBlinkMs = System.currentTimeMillis();
    caretVisible = true;
  }

  private static String formatNumber(Number value) {
    if (value instanceof Double d) return trimZeros(String.valueOf(d));
    if (value instanceof Float f) return trimZeros(String.valueOf(f));
    return String.valueOf(value);
  }

  private static String trimZeros(String text) {
    if (text.indexOf('.') < 0) return text;
    text = text.replaceAll("0+$", "");
    if (text.endsWith(".")) text = text.substring(0, text.length() - 1);
    return text;
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    super.mouseClicked(mouseX, mouseY, mouseButton);
    if (!isHovering(mouseX, mouseY)) return;
    if (mouseButton == InputConstants.MOUSE_BUTTON_MIDDLE) {
      // Middle click puts a caret in the drawn value instead of dragging for it.
      openEditor();
      ClickGui.getClickGui().openPopup(new NumberEditPopup(this));
      return;
    }
    if (mouseButton == InputConstants.MOUSE_BUTTON_LEFT) {
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
