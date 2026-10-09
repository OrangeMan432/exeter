package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class Step extends ToggleableModule {

  private static final double DEFAULT_STEP_HEIGHT = 0.6;

  private final NumberProperty<Double> height =
      new NumberProperty<Double>(2.0, 0.5, 10.0, "Height");

  public Step() {
    super("Step", new String[] {"step", "stepup"}, 0xFF0000, ModuleType.MOVEMENT);
    setDescription("Steps up full blocks instantly without jumping.");
    this.offerProperties(height);
    height.setDescription("Maximum block height to step up.");

    this.listeners.add(
        new Listener<TickEvent>("step_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Step.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    updateStep();
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    resetStep();
  }

  private void onTick() {
    if (minecraft.player == null) return;
    updateStep();
  }

  private void updateStep() {
    if (minecraft.player == null) return;
    AttributeInstance attr = minecraft.player.getAttribute(Attributes.STEP_HEIGHT);
    if (attr == null) return;
    attr.setBaseValue(height.getValue());
  }

  private void resetStep() {
    if (minecraft.player == null) return;
    AttributeInstance attr = minecraft.player.getAttribute(Attributes.STEP_HEIGHT);
    if (attr == null) return;
    attr.setBaseValue(DEFAULT_STEP_HEIGHT);
  }
}
