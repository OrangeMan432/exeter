package me.larp.client.command.impl.client;

import java.util.StringJoiner;
import me.larp.client.command.Argument;
import me.larp.client.command.Command;
import me.larp.client.core.Larp;

public final class Help extends Command {
  public Help() {
    super(new String[] {"help", "halp", "autism", "how"}, new Argument[0]);
  }

  @Override
  public String dispatch() {
    StringJoiner stringJoiner = new StringJoiner(", ");
    Larp.getInstance()
        .getCommandManager()
        .getRegistry()
        .forEach(command -> stringJoiner.add(command.getAliases()[0]));
    return String.format(
        "Commands (%s) %s",
        Larp.getInstance().getCommandManager().getRegistry().size(), stringJoiner.toString());
  }
}
