package me.larp.client.events;

import me.larp.api.event.Event;

public class InputEvent extends Event {
  private final Type type;
  private int key;

  public InputEvent(Type type) {
    this.type = type;
    this.key = 0;
  }

  public InputEvent(Type type, int key) {
    this.type = type;
    this.key = key;
  }

  public Type getType() {
    return this.type;
  }

  public int getKey() {
    return this.key;
  }

  public static enum Type {
    KEYBOARD_KEY_PRESS,
    MOUSE_LEFT_CLICK,
    MOUSE_MIDDLE_CLICK,
    MOUSE_RIGHT_CLICK;
  }
}
