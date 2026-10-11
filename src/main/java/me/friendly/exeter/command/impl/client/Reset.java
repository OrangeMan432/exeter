package me.friendly.exeter.command.impl.client;

import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.config.ExeterConfig;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.properties.Property;

public final class Reset extends Command {
  public Reset() {
    super(new String[] {"reset"}, new Argument("module"));
    setDescription("Reset a module's settings to defaults");
  }

  @Override
  public String dispatch() {
    Module module =
        Exeter.getInstance()
            .getModuleManager()
            .getModuleByAlias(this.getArgument("module").getValue());
    if (module == null) {
      return "No such module exists.";
    }
    int count = 0;
    for (Property<?> property : module.getProperties()) {
      property.resetToDefault();
      count++;
    }
    ExeterConfig.getInstance().saveModule(module);
    return String.format("&e%s&7 settings reset to defaults &8(%d)&7.", module.getLabel(), count);
  }
}
