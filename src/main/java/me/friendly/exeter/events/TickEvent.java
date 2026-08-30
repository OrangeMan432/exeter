package me.friendly.exeter.events;

import me.friendly.api.event.Event;
import me.friendly.api.event.Stage;

public class TickEvent extends Event {
  private final Stage stage;

  public TickEvent(Stage stage) {
    this.stage = stage;
  }

  public Stage getStage() {
    return stage;
  }
}
