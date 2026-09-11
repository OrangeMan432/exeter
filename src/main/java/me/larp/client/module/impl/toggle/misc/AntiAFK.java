package me.larp.client.module.impl.toggle.misc;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;

/** Spins, sneaks, and jumps on a timer so servers don't kick for AFK. */
public class AntiAFK extends ToggleableModule {

  private final NumberProperty<Integer> actionDelay =
      new NumberProperty<Integer>(100, 20, 600, "Action Delay");
  private final Property<Boolean> spin = new Property<Boolean>(true, "Spin");
  private final Property<Boolean> sneak = new Property<Boolean>(true, "Sneak");
  private final Property<Boolean> jump = new Property<Boolean>(false, "Jump");

  private int tickCounter;
  private int step;

  public AntiAFK() {
    super("AntiAFK", new String[] {"antiafk", "afk"}, 0x00FFFF, ModuleType.MISCELLANEOUS);
    setDescription("Anti-AFK movement patterns.");
    offerProperties(actionDelay, spin, sneak, jump);
    this.listeners.add(
        new Listener<TickEvent>("antiafk_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AntiAFK.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
    step = 0;
  }

  private void onTick() {
    if (minecraft.player == null) return;
    if (tickCounter++ < actionDelay.getValue()) return;
    tickCounter = 0;
    step++;
    if (spin.getValue()) {
      minecraft.player.setYRot(minecraft.player.getYRot() + 90f * (step % 2 == 0 ? 1 : -1));
    }
    if (sneak.getValue()) {
      minecraft.player.setShiftKeyDown(!minecraft.player.isShiftKeyDown());
    }
    if (jump.getValue() && minecraft.player.onGround()) {
      minecraft.player.jumpFromGround();
    }
  }
}
