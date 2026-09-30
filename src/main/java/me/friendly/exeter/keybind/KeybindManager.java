package me.friendly.exeter.keybind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import me.friendly.api.event.Listener;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.api.event.Stage;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/**
 * Polls LWJGL key state on client tick and fires keybinds on rising edges. Beta has no keybind
 * event system, so edge detection lives here.
 */
public final class KeybindManager extends ListRegistry<Keybind> {

  private final Map<String, Boolean> wasDown = new HashMap<String, Boolean>();

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("keybind_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          KeybindManager.this.poll();
        }
      };

  public KeybindManager() {
    this.registry = new ArrayList<Keybind>();
    Exeter.getInstance().getEventManager().register(tickListener);
  }

  private void poll() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc != null && mc.currentScreen != null) {
      return;
    }
    for (Keybind keybind : getRegistry()) {
      int key = keybind.getKey();
      if (key == 0) continue;
      boolean down = false;
      try {
        down = Keyboard.isKeyDown(key);
      } catch (Exception ignored) {
      }
      boolean was = wasDown.containsKey(keybind.getLabel())
          ? ((Boolean) wasDown.get(keybind.getLabel())).booleanValue()
          : false;
      if (down && !was) {
        try {
          keybind.onPressed();
        } catch (Exception ignored) {
        }
      }
      wasDown.put(keybind.getLabel(), Boolean.valueOf(down));
    }
  }

  public Keybind getKeybindByLabel(String label) {
    for (Keybind keybind : getRegistry()) {
      if (keybind.getLabel().equalsIgnoreCase(label)) {
        return keybind;
      }
    }
    return null;
  }
}
