package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;

public class Velocity extends ToggleableModule {

  public Velocity() {
    super("Velocity", new String[] {"velocity", "velocity-cancel"}, 0xFF0000, ModuleType.MOVEMENT);
    setDescription("Cancels knockback from attacks.");
    offerProperties();
  }
}
