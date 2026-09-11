package me.larp.client.module.impl.toggle.movement;

import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.Property;

/**
 * Removes the using-item movement slowdown via a LocalPlayer mixin
 * (isSlowDueToUsingItem forced false while running).
 */
public class NoSlow extends ToggleableModule {

  private final Property<Boolean> onlyFood =
      new Property<Boolean>(false, "Only Food");

  public NoSlow() {
    super("NoSlow", new String[] {"noslow", "no-slow"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Removes slowdown while eating or drinking.");
    offerProperties(onlyFood);
  }

  public boolean isFoodOnly() {
    return onlyFood.getValue();
  }
}
