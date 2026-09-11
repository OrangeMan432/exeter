package me.larp.client.command.impl.client;

import me.larp.api.interfaces.Toggleable;
import me.larp.client.command.Argument;
import me.larp.client.command.Command;
import me.larp.client.core.Larp;
import me.larp.client.module.Module;
import me.larp.client.module.ToggleableModule;

public final class Toggle extends Command {
  public Toggle() {
    super(new String[] {"toggle", "t"}, new Argument("module"));
  }

  @Override
  public String dispatch() {
    Module module =
        Larp.getInstance()
            .getModuleManager()
            .getModuleByAlias(this.getArgument("module").getValue());
    if (module == null) {
      return "No such module exists.";
    }
    if (!(module instanceof Toggleable)) {
      return "That module is not toggleable.";
    }
    ToggleableModule toggleableModule = (ToggleableModule) module;
    toggleableModule.toggle();
    return String.format(
        "&e%s&7 has been %s&7.",
        toggleableModule.getLabel(), toggleableModule.isRunning() ? "&aenabled" : "&cdisabled");
  }
}
