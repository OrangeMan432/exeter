package me.friendly.exeter.events;

import com.mojang.blaze3d.platform.Window;
import me.friendly.api.event.Event;

public class RenderGameInfoEvent extends Event {
  private Window window;

  public RenderGameInfoEvent(Window window) {
    this.window = window;
  }

  public Window getWindow() {
    return this.window;
  }
}
