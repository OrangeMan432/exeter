package me.larp.client.command.impl.client;

import java.util.StringJoiner;
import me.larp.api.interfaces.Toggleable;
import me.larp.client.command.Argument;
import me.larp.client.command.Command;
import me.larp.client.core.Larp;
import me.larp.client.module.Module;
import me.larp.client.module.ToggleableModule;

public final class Modules extends Command {
  public Modules() {
    super(new String[] {"modules", "mods", "ms", "ml", "lm"}, new Argument[0]);
  }

  @Override
  public String dispatch() {
    StringJoiner stringJoiner = new StringJoiner(", ");
    java.util.List<Module> modules = Larp.getInstance().getModuleManager().getRegistry();
    modules.sort((mod1, mod2) -> mod1.getLabel().compareTo(mod2.getLabel()));
    modules.forEach(
        module -> {
          if (module instanceof Toggleable) {
            ToggleableModule toggleableModule = (ToggleableModule) module;
            stringJoiner.add(
                String.format(
                    "%s%s&7",
                    toggleableModule.isRunning() ? "&a" : "&c", toggleableModule.getLabel()));
          }
        });
    return String.format(
        "Modules (%s) %s",
        Larp.getInstance().getModuleManager().getRegistry().size(), stringJoiner.toString());
  }
}
