package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;

public class NoFall extends ToggleableModule {

  private final NumberProperty<Double> minDistance =
      new NumberProperty<Double>(3.0, 0.5, 20.0, "Min Distance");

  public NoFall() {
    super("NoFall", new String[] {"nofall", "no-fall"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Prevents fall damage by resetting fall distance.");
    offerProperties(minDistance);
    this.listeners.add(
        new Listener<TickEvent>("nofall_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            NoFall.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    // Elytra flight and liquids manage their own fall state.
    if (minecraft.player.isFallFlying()) return;
    if (minecraft.player.isInWater() || minecraft.player.isInLava()) return;
    if (minecraft.player.onGround()) return;
    if (minecraft.player.fallDistance >= minDistance.getValue()) {
      minecraft.player.fallDistance = 0;
    }
  }
}
