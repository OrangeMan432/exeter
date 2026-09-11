package me.larp.client.events;

import me.larp.api.event.Event;

public class NightVisionEvent extends Event {
  private final Type type;

  public NightVisionEvent(Type type) {
    this.type = type;
  }

  public Type getType() {
    return this.type;
  }

  public static enum Type {
    TIME,
    VISUAL;
  }
}
