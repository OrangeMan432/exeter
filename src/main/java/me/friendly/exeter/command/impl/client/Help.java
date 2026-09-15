package me.friendly.exeter.command.impl.client;

import java.util.StringJoiner;
import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.core.Exeter;

public final class Help extends Command {
  public Help() {
    super(new String[] {"help", "halp", "autism", "how"}, new Argument("command"));
    setDescription("Show help");
  }

  @Override
  public String dispatch(String[] input) {
    if (input.length >= 2) {
      String target = input[1].replace(".", "").toLowerCase();
      for (var cmd : Exeter.getInstance().getCommandManager().getRegistry()) {
        for (String alias : cmd.getAliases()) {
          if (alias.equalsIgnoreCase(target)) {
            return getHelpFor(cmd);
          }
        }
      }
      return "Unknown command &e" + target + "&7. Try &e.help&7.";
    }
    return dispatch();
  }

  @Override
  public String dispatch() {
    StringJoiner stringJoiner = new StringJoiner(", ");
    Exeter.getInstance()
        .getCommandManager()
        .getRegistry()
        .forEach(command -> stringJoiner.add(command.getAliases()[0]));
    return String.format(
        "Commands (%s) %s",
        Exeter.getInstance().getCommandManager().getRegistry().size(), stringJoiner.toString());
  }

  private String getHelpFor(Command cmd) {
    String name = cmd.getAliases()[0];
    String desc = cmd.getDescription();
    StringBuilder sb = new StringBuilder();
    sb.append("&e.").append(name).append("&7");
    if (!desc.isEmpty()) sb.append(" - ").append(desc);
    var subs = cmd.getSubCommands();
    if (!subs.isEmpty()) {
      sb.append(" &7Subcommands: ");
      StringJoiner join = new StringJoiner("&7, ");
      for (var sub : subs) {
        String entry = "&e" + sub.name();
        if (!sub.args().isEmpty()) entry += " " + sub.args();
        if (!sub.description().isEmpty()) entry += "&7 (" + sub.description() + ")";
        join.add(entry);
      }
      sb.append(join.toString());
    } else {
      StringJoiner args = new StringJoiner(" ");
      for (var a : cmd.getArguments()) {
        args.add("[" + a.getLabel() + "]");
      }
      if (args.length() > 0) sb.append(" &e").append(args).append("&7");
      sb.append(" - aliases: ").append(String.join(", ", cmd.getAliases()));
    }
    return sb.toString();
  }
}
