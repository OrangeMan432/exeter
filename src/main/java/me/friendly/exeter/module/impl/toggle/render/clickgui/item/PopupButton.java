package me.friendly.exeter.module.impl.toggle.render.clickgui.item;

import me.friendly.exeter.properties.PopupProperty;

public class PopupButton extends Button {
  private final PopupProperty property;

  public PopupButton(PopupProperty property, boolean child) {
    super(property.getAliases()[0] + "...");
    this.property = property;
  }

  @Override
  public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
      this.property.getOpenAction().run();
    }
  }

  public PopupProperty getProperty() {
    return this.property;
  }
}
