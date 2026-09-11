package me.larp.client.command.impl.client;

import me.larp.client.command.Argument;
import me.larp.client.command.Command;
import me.larp.client.core.Larp;

public final class Prefix extends Command {
  public Prefix() {
    super(new String[] {"prefix"}, new Argument("character"));
  }

  @Override
  public String dispatch() {
    String prefix = getArgument("character").getValue();
    Larp.getInstance().getCommandManager().setPrefix(prefix);
    return String.format("&e%s&7 is now your prefix.", prefix);
  }
}
