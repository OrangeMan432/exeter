package me.friendly.exeter.command.impl.client;

import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.core.Exeter;

public final class Prefix extends Command {
  public Prefix() {
    super(new String[] {"prefix", "p"}, new Argument("prefix"));
    setDescription("Sets the command prefix");
  }

  @Override
  public String dispatch() {
    String prefix = this.getArgument("prefix").getValue();
    Exeter.getInstance().getCommandManager().setPrefix(prefix);
    return "Prefix set to " + prefix;
  }
}
