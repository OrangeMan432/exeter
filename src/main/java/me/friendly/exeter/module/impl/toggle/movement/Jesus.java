package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;

/** Walk on water: holds you at the surface while moving. */
public class Jesus extends ToggleableModule {

  private final Property<Boolean> dip =
      new Property<Boolean>(true, "Dip");

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
    if (!minecraft.player.isInWater()) return;
    if (minecraft.player.isShiftKeyDown()) {
      // Sneak to dive like vanilla.
      return;
    }
    if (minecraft.player.getDeltaMovement().y < 0.0) {
      minecraft.player.setDeltaMovement(
          minecraft.player.getDeltaMovement().x,
          0.12,
          minecraft.player.getDeltaMovement().z);
      if (dip.getValue()) {
        minecraft.player.setDeltaMovement(
            minecraft.player.getDeltaMovement().x * 1.1,
            minecraft.player.getDeltaMovement().y,
            minecraft.player.getDeltaMovement().z * 1.1);
      }
    }
  }
}
