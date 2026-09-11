package me.larp.client.events;

import com.mojang.blaze3d.platform.Window;
import me.larp.api.event.Event;

public class RenderGameInfoEvent extends Event {
  private Window window;

  public RenderGameInfoEvent(Window window) {
    this.window = window;
  }

  public Window getWindow() {
    return this.window;
  }
}
