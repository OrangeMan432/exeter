package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;

public class Step extends ToggleableModule {

  private static final float DEFAULT_STEP_HEIGHT = 0.5F;

  private final NumberProperty<Double> height =
      new NumberProperty<Double>(2.0, 0.5, 10.0, "Height", "height");

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("step_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          Step.this.onTick();
        }
      };

  public Step() {
    super("Step", new String[] {"step", "stepup"}, 0xFF0000, ModuleType.MOVEMENT);
    setDescription("Steps up full blocks instantly without jumping.");
    offerProperties(height);
    this.listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    updateStep();
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    if (minecraft() != null && minecraft().player != null) {
      minecraft().player.field_1641 = DEFAULT_STEP_HEIGHT;
    }
  }

  private void onTick() {
    if (minecraft() == null || minecraft().player == null) {
      return;
    }
    updateStep();
  }

  private void updateStep() {
    if (minecraft() == null || minecraft().player == null) {
      return;
    }
    // field_1641 is stepHeight (verified: gates step-up block in Entity.move).
    minecraft().player.field_1641 = height.getValue().floatValue();
  }
}
