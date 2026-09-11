package me.larp.client.command.impl.client;

import me.larp.client.command.Argument;
import me.larp.client.command.Command;
import me.larp.client.core.Larp;
import me.larp.client.module.impl.toggle.movement.Goto;

/** .goto <x> <z> walks to coordinates via the Goto module. */
public final class GotoCommand extends Command {
  public GotoCommand() {
    super(new String[] {"goto", "path"}, new Argument("x"), new Argument("z"));
  }

  @Override
  public String dispatch() {
    Goto gotoModule = Larp.getInstance().getModuleManager().getModule(Goto.class);
    if (gotoModule == null) return "Goto module missing.";
    if (minecraft.player == null) return "Not in game.";
    double x;
    double z;
    try {
      x = parse(this.getArgument("x").getValue(), minecraft.player.getX());
      z = parse(this.getArgument("z").getValue(), minecraft.player.getZ());
    } catch (Exception e) {
      return "Usage: .goto <x> <z> (~ for relative)";
    }
    gotoModule.setTarget(x, z);
    return String.format("Walking to &e%.0f %.0f&7.", x, z);
  }

  private double parse(String text, double current) {
    if (text.startsWith("~")) {
      return current + (text.length() > 1 ? Double.parseDouble(text.substring(1)) : 0.0);
    }
    return Double.parseDouble(text);
  }
}
