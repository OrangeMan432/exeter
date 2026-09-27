package me.friendly.exeter.properties;

/**
 * A clickable action exposed in module settings. Renders as a button in ClickGUI and runs its
 * action when clicked. Never persisted to config files.
 */
public class ActionProperty extends Property<Runnable> {

  public ActionProperty(String label, Runnable action) {
    super(action, label);
  }

  public void run() {
    if (getValue() != null) {
      getValue().run();
    }
  }
}
