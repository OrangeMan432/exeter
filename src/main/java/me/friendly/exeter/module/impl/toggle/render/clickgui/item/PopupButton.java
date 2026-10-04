package me.friendly.exeter.module.impl.toggle.render.clickgui.item;

import com.mojang.blaze3d.platform.InputConstants;
import me.friendly.exeter.properties.PopupProperty;

public class PopupButton extends Button {
  private final PopupProperty property;

  public PopupButton(PopupProperty property) {
    super(property.getAliases()[0] + "...");
    this.property = property;
  }

  @Override
  public boolean isVisible() {
    return property == null || property.isVisible();
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (mouseButton == InputConstants.MOUSE_BUTTON_LEFT && this.isHovering(mouseX, mouseY)) {
      this.property.getOpenAction().run();
    }
  }

  public PopupProperty getProperty() {
    return this.property;
  }
}
