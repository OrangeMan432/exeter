package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;

/** Falls faster than vanilla for quick drops and dodge-downs. */
public class FastFall extends ToggleableModule {

  private final NumberProperty<Double> speed =
      new NumberProperty<Double>(2.0, 1.0, 5.0, "Speed");

  public FastFall() {
    super("FastFall", new String[] {"fastfall", "fast-fall"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Increases fall speed.");
    offerProperties(speed);
    this.listeners.add(
        new Listener<TickEvent>("fastfall_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            FastFall.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (minecraft.player.onGround()) return;
    if (minecraft.player.isInWater() || minecraft.player.isInLava()) return;
    if (minecraft.player.isFallFlying()) return;
    if (minecraft.player.getDeltaMovement().y >= 0.0) return;
    minecraft.player.setDeltaMovement(
        minecraft.player.getDeltaMovement().x,
        minecraft.player.getDeltaMovement().y * speed.getValue(),
        minecraft.player.getDeltaMovement().z);
  }
}
