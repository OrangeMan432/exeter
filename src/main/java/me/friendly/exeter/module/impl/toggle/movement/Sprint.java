package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;

public class Sprint extends ToggleableModule {

  private final Property<Boolean> omni = new Property<>(false, "Omnidirectional");
  private final Property<Boolean> hungerCheck = new Property<>(true, "Hunger Check");

  public Sprint() {
    super("Sprint", new String[] {"sprint", "autosprint"}, 0xFF0000, ModuleType.MOVEMENT);
    setDescription("Automatically sprints in the direction you are moving.");
    offerProperties(omni, hungerCheck);

    this.listeners.add(
        new Listener<TickEvent>("sprint_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.POST) return;
            Sprint.this.onTick();
          }
        });
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    if (minecraft.player != null) {
      minecraft.player.setSprinting(false);
    }
  }

  private void onTick() {
    if (minecraft.player == null) return;
    if (minecraft.player.isUsingItem()) return;
    // Vanilla refuses sprint at 6 hunger or less; don't fight the server.
    if (hungerCheck.getValue() && minecraft.player.getFoodData().getFoodLevel() <= 6) {
      minecraft.player.setSprinting(false);
      return;
    }

    boolean moving;
    if (omni.getValue()) {
      moving =
          minecraft.player.input.hasForwardImpulse()
              || minecraft.player.input.keyPresses.left()
              || minecraft.player.input.keyPresses.right()
              || minecraft.player.input.keyPresses.backward();
    } else {
      moving = minecraft.player.input.hasForwardImpulse();
    }

    if (moving) {
      minecraft.player.setSprinting(true);
    } else {
      minecraft.player.setSprinting(false);
    }
  }
}
