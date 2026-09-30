package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.util.PlayerUtil;

public class Speed extends ToggleableModule {

  private final NumberProperty<Double> factor =
      new NumberProperty<Double>(1.5, 1.0, 5.0, "Factor", "factor");

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("speed_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          Speed.this.onTick();
        }
      };

  public Speed() {
    super("Speed", new String[] {"speed"}, 0xFF5555, ModuleType.MOVEMENT);
    setDescription("Multiplies horizontal velocity while moving.");
    offerProperties(factor);
    this.listeners.add(tickListener);
  }

  private void onTick() {
    if (minecraft() == null || minecraft().player == null) {
      return;
    }
    if (!PlayerUtil.isMoving()) {
      return;
    }
    double factorValue = factor.getValue().doubleValue();
    minecraft().player.velocityX *= factorValue;
    minecraft().player.velocityZ *= factorValue;
  }
}
