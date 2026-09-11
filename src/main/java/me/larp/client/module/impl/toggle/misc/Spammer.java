package me.larp.client.module.impl.toggle.misc;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;

/** Repeats a chat message on a timer. Set text with .spam <message>. */
public class Spammer extends ToggleableModule {

  private static String message = "Larp Client on top";

  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(100, 10, 1200, "Delay");

  private int tickCounter;

  public Spammer() {
    super("Spammer", new String[] {"spammer", "spam"}, 0x00FFFF, ModuleType.MISCELLANEOUS);
    setDescription("Repeats chat via .spam <message>.");
    offerProperties(delay);
    this.listeners.add(
        new Listener<TickEvent>("spammer_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Spammer.this.onTick();
          }
        });
  }

  public static void setMessage(String text) {
    message = text;
  }

  public static String getMessage() {
    return message;
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = delay.getValue();
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.getConnection() == null) return;
    if (tickCounter++ < delay.getValue()) return;
    tickCounter = 0;
    if (message.startsWith("/")) return;
    minecraft.getConnection().sendChat(message + " [" + (int) (Math.random() * 9999) + "]");
  }
}
