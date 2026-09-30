package me.friendly.exeter.events;

import me.friendly.api.event.Stage;
import me.friendly.api.event.StageEvent;

/** Fired around the client tick. */
public class TickEvent extends StageEvent {
  public TickEvent(Stage stage) {
    super(stage);
  }
}
