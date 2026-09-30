package me.friendly.exeter.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Command {
  private final String[] aliases;
  private final Argument[] arguments;
  private String description = "";
  private final List<SubCommand> subCommands = new ArrayList<SubCommand>();

  public static final class SubCommand {
    private final String name;
    private final String args;
    private final String description;

    public SubCommand(String name, String args, String description) {
      this.name = name;
      this.args = args;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getArgs() {
      return args;
    }

    public String getDescription() {
      return description;
    }
  }

  public Command(String[] aliases, Argument... arguments) {
    this.aliases = aliases;
    this.arguments = arguments;
  }

  protected void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  protected void addSubCommand(String name, String args, String description) {
    subCommands.add(new SubCommand(name, args, description));
  }

  public List<SubCommand> getSubCommands() {
    return Collections.unmodifiableList(subCommands);
  }

  /**
   * Called when a chat message starting with the prefix is sent.
   *
   * @param input the input, split on spaces with element 0 being the command itself.
   */
  public String dispatch(String[] input) {
    Argument[] arguments = this.getArguments();
    boolean valid = false;
    if (input.length < arguments.length) {
      return String.format("%s %s", input[0], this.getSyntax());
    }
    if (input.length - 1 > arguments.length) {
      return String.format("Maximum number of arguments is %s.", Integer.valueOf(arguments.length));
    }
    if (arguments.length > 0) {
      for (int index = 0; index < arguments.length; ++index) {
        Argument argument = arguments[index];
        argument.setPresent(index < input.length);
        argument.setValue(input[index + 1]);
        valid = argument.isPresent();
      }
    } else {
      valid = true;
    }
    return valid ? this.dispatch() : "Invalid argument(s).";
  }

  public final String[] getAliases() {
    return this.aliases;
  }

  public final Argument[] getArguments() {
    return this.arguments;
  }

  public Argument getArgument(String label) {
    for (Argument argument : arguments) {
      if (!label.equalsIgnoreCase(argument.getLabel())) continue;
      return argument;
    }
    return null;
  }

  /**
   * Returns the syntax of the Command. Called, and printed to chat when the user enters a command
   * with incorrect syntax.
   *
   * @return the syntax of the command, as a String
   */
  public String getSyntax() {
    StringBuilder builder = new StringBuilder();
    for (Argument argument : arguments) {
      if (builder.length() > 0) builder.append(" ");
      builder.append("[").append(argument.getLabel()).append("]");
    }
    return builder.toString();
  }

  public abstract String dispatch();
}
