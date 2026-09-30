package me.friendly.exeter.command.impl.client;

import me.friendly.api.interfaces.Toggleable;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;

public final class Modules extends Command {
  public Modules() {
    super(new String[] {"modules", "mods", "list"});
    setDescription("Lists modules");
  }

  @Override
  public String dispatch() {
    StringBuilder enabled = new StringBuilder("On: ");
    StringBuilder disabled = new StringBuilder("Off: ");
    boolean firstOn = true;
    boolean firstOff = true;
    for (Module module : Exeter.getInstance().getModuleManager().getRegistry()) {
      if (!(module instanceof Toggleable)) continue;
      boolean running = ((Toggleable) module).isRunning();
      if (running) {
        if (!firstOn) enabled.append(", ");
        firstOn = false;
        enabled.append(module.getLabel());
      } else {
        if (!firstOff) disabled.append(", ");
        firstOff = false;
        disabled.append(module.getLabel());
      }
    }
    return enabled.toString() + " | " + disabled.toString();
  }
}
