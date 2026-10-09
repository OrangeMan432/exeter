package me.friendly.exeter.module.impl.toggle.render.clickgui.item.properties;

import me.friendly.exeter.module.impl.toggle.render.clickgui.item.Button;
import me.friendly.exeter.module.impl.toggle.render.clickgui.item.PropertyItem;
import me.friendly.exeter.properties.ActionProperty;

/** ClickGUI row for an {@link ActionProperty}: stateless button that runs its action on click. */
public class ActionButton extends Button implements PropertyItem {

  private final ActionProperty property;

  public ActionButton(ActionProperty property) {
    super(property.getAliases()[0]);
    this.property = property;
  }

  @Override
  public boolean isVisible() {
    return property == null || property.isVisible();
  }

  @Override
  public ActionProperty getProperty() {
    return this.property;
  }

  @Override
  public void toggle() {
    property.run();
  }

  @Override
  public boolean getState() {
    return false;
  }
}
