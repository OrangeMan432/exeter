package me.larp.client.module.impl.toggle.movement;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.mixin.MixinDeltaTracker;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;

/**
 * Scales client tick speed via the render timer. UNVERIFIED in-game:
 * start at low values. Defaults to no-op.
 */
public class Timer extends ToggleableModule {

  private final NumberProperty<Double> speed =
      new NumberProperty<Double>(1.0, 0.1, 5.0, "Speed");

  public Timer() {
    super("Timer", new String[] {"timer", "tickshift"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Client tick speed multiplier. Test carefully.");
    offerProperties(speed);
    this.listeners.add(
        new Listener<TickEvent>("timer_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Timer.this.onTick();
          }
        });
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    setMsPerTick(50.0f);
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    setMsPerTick((float) (50.0 / speed.getValue()));
    setTag("Timer [" + String.format("%.1f", speed.getValue()) + "x]");
  }

  private void setMsPerTick(float value) {
    try {
      Object tracker = minecraft.getDeltaTracker();
      if (tracker != null) {
        MixinDeltaTracker.of((net.minecraft.client.DeltaTracker) tracker).setMsPerTick(value);
      }
    } catch (Exception e) {
      // Timer internals differ: fail silent, never crash the tick loop.
    }
  }
}
