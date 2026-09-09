package me.friendly.exeter.properties;

public class PopupProperty extends Property<Boolean> {
  private final Runnable openAction;

  public PopupProperty(String label, Runnable openAction) {
    super(false, label);
    this.openAction = openAction;
  }

  public Runnable getOpenAction() {
    return this.openAction;
  }
}
