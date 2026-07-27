package me.friendly.exeter.events;

import me.friendly.api.event.Event;
import com.mojang.blaze3d.platform.Window;

public class RenderGameInfoEvent
extends Event {
    private Window window;

    public RenderGameInfoEvent(Window window) {
        this.window = window;
    }

    public Window getWindow() {
        return this.window;
    }
}

