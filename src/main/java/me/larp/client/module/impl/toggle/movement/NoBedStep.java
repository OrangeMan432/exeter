package me.larp.client.module.impl.toggle.movement;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.core.Larp;
import me.larp.client.events.TickEvent;
import me.larp.client.module.Module;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class NoBedStep extends ToggleableModule {

  private static final double DEFAULT_STEP_HEIGHT = 0.6;

  public NoBedStep() {
    super("NoBedStep", new String[] {"nobedstep", "nobed"}, 0xFF0000, ModuleType.MOVEMENT);
    setDescription("Removes the step height increase from bed mining.");

    this.listeners.add(
        new Listener<TickEvent>("nobedstep_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            NoBedStep.this.onTick();
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
    Module stepModule = Larp.getInstance().getModuleManager().getModule(Step.class);
    boolean stepRunning = stepModule instanceof ToggleableModule t && t.isRunning();
    if (stepRunning) return;
    updateStep();
  }

  private void updateStep() {
    if (minecraft.player == null) return;
    AttributeInstance attr = minecraft.player.getAttribute(Attributes.STEP_HEIGHT);
    if (attr == null) return;
    attr.setBaseValue(0.0);
  }

  private void resetStep() {
    if (minecraft.player == null) return;
    AttributeInstance attr = minecraft.player.getAttribute(Attributes.STEP_HEIGHT);
    if (attr == null) return;
    attr.setBaseValue(DEFAULT_STEP_HEIGHT);
  }
}
