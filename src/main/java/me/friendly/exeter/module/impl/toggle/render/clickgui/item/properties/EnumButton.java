package me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import me.friendly.api.minecraft.render.RenderMethods;
import me.friendly.api.minecraft.render.font.FontUtil;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.impl.active.render.Colors;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Button;
import me.friendly.exeter.properties.EnumProperty;

public class EnumButton extends Button {
  private EnumProperty property;
  private final Module module;
  private final boolean child;
  private static final float CHILD_OFFSET = 1.0f;

  public EnumButton(EnumProperty property, Module module, boolean child) {
    super(property.getAliases()[0]);
    this.property = property;
    this.module = module;
    this.child = child;
  }

  @Override
  public boolean isVisible() {
    return property == null || property.isVisible();
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
      if (mouseButton == InputConstants.MOUSE_BUTTON_LEFT) {
        this.property.increment();
      } else if (mouseButton == InputConstants.MOUSE_BUTTON_RIGHT) {
        this.property.decrement();
      } else {
        return;
      }
      if (this.module instanceof me.friendly.exeter.module.impl.toggle.render.ClickGui guiMod
          && Arrays.asList(this.property.getAliases()).contains("Panel Alignment")) {
        guiMod.onAlignmentChanged();
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
