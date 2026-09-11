package me.larp.client.module.impl.toggle.movement;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.Property;

/** Walk on water: holds you at the surface while moving. */
public class Jesus extends ToggleableModule {

  private final Property<Boolean> dip = new Property<Boolean>(true, "Dip");

  public Jesus() {
    super("Jesus", new String[] {"jesus", "water-walk"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Walk on water.");
    offerProperties(dip);
    this.listeners.add(
        new Listener<TickEvent>("jesus_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Jesus.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (!minecraft.player.isInWater() && !minecraft.player.isInLava()) return;
    if (minecraft.player.isShiftKeyDown()) {
      // Sneak to dive like vanilla.
      return;
    }
    if (minecraft.player.getDeltaMovement().y < 0.0) {
      double hSpeed =
          Math.sqrt(
              minecraft.player.getDeltaMovement().x * minecraft.player.getDeltaMovement().x
                  + minecraft.player.getDeltaMovement().z * minecraft.player.getDeltaMovement().z);
      double boost = hSpeed < 0.3 && dip.getValue() ? 1.1 : 1.0;
      minecraft.player.setDeltaMovement(
          minecraft.player.getDeltaMovement().x * boost,
          0.12,
          minecraft.player.getDeltaMovement().z * boost);
    }
  }
}
