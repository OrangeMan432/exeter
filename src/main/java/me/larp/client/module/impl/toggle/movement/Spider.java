package me.larp.client.module.impl.toggle.movement;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;

/** Climbs walls by lifting you while colliding horizontally. */
public class Spider extends ToggleableModule {

  private final NumberProperty<Double> speed =
      new NumberProperty<Double>(0.2, 0.05, 1.0, "Speed");

  public Spider() {
    super("Spider", new String[] {"spider", "wall-climb"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Climbs walls on contact.");
    offerProperties(speed);
    this.listeners.add(
        new Listener<TickEvent>("spider_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Spider.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (!minecraft.player.horizontalCollision) return;
    if (minecraft.player.getDeltaMovement().y > speed.getValue()) return;
    minecraft.player.setDeltaMovement(
        minecraft.player.getDeltaMovement().x,
        speed.getValue(),
        minecraft.player.getDeltaMovement().z);
  }
}
