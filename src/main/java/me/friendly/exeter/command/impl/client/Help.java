package me.friendly.exeter.command.impl.client;

import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.core.Exeter;

public final class Help extends Command {
  public Help() {
    super(new String[] {"help", "h"});
    setDescription("Lists commands");
  }

  @Override
  public String dispatch() {
    StringBuilder builder = new StringBuilder("Commands: ");
    boolean first = true;
    for (Command command : Exeter.getInstance().getCommandManager().getRegistry()) {
      if (!first) {
        builder.append(", ");
      }
      first = false;
      builder.append(command.getAliases()[0]);
    }
    return builder.toString();
  }
}
