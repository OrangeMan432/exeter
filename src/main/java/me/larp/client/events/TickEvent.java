package me.larp.client.events;

import me.larp.api.event.Event;
import me.larp.api.event.Stage;

public class TickEvent extends Event {
  private final Stage stage;

  public TickEvent(Stage stage) {
    this.stage = stage;
  }

  public Stage getStage() {
    return stage;
  }
}
