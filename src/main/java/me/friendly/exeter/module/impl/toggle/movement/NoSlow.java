package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;

/**
 * Removes the using-item movement slowdown via a LocalPlayer mixin
 * (isSlowDueToUsingItem forced false while running).
 */
public class NoSlow extends ToggleableModule {

  public NoSlow() {
    super("NoSlow", new String[] {"noslow", "no-slow"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Removes slowdown while eating or drinking.");
  }
}
