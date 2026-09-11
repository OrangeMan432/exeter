package me.larp.client.command.impl.client;

import java.util.StringJoiner;
import me.larp.client.command.Argument;
import me.larp.client.command.Command;
import me.larp.client.core.Larp;
import me.larp.client.module.Module;
import me.larp.client.presets.Preset;

public final class Presets extends Command {
  public Presets() {
    super(new String[] {"preset", "presets"}, new Argument("module"), new Argument("preset"));
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
    if (module.getPresets().size() < 1) {
      return "That module has no presets.";
    }
    Preset preset = module.getPresetByLabel(this.getArgument("preset").getValue());
    if (preset == null) {
      StringJoiner stringJoiner = new StringJoiner(", ");
      for (Preset prese : module.getPresets()) {
        stringJoiner.add(prese.getLabel());
      }
      return String.format("Try: %s.", stringJoiner.toString());
    }
    preset.onSet();
    return String.format("Loaded &e%s&7 preset for &e%s&7.", preset.getLabel(), module.getLabel());
  }
}
