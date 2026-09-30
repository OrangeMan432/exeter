package me.friendly.exeter.command;

import java.util.ArrayList;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.command.impl.client.Bind;
import me.friendly.exeter.command.impl.client.Friends;
import me.friendly.exeter.command.impl.client.Help;
import me.friendly.exeter.command.impl.client.Modules;
import me.friendly.exeter.command.impl.client.Prefix;
import me.friendly.exeter.command.impl.client.Toggle;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.logging.Logger;

public final class CommandManager extends ListRegistry<Command> {
  private String prefix = ".";

  public CommandManager() {
    this.registry = new ArrayList<Command>();
    this.register(new Toggle());
    this.register(new Help());
    this.register(new Modules());
    this.register(new Prefix());
    this.register(new Friends.Add());
    this.register(new Friends.Remove());
    this.register(new Bind());
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  /** Dispatches a raw chat message, returning the response lines. */
  public String dispatchDirect(String message) {
    String trimmed = message.trim();
    if (!trimmed.startsWith(prefix) || trimmed.length() <= prefix.length()) {
      return "No command was entered.";
    }
    String[] arguments = trimmed.split(" ");
    String execute = arguments[0].substring(prefix.length());
    for (Command command : getRegistry()) {
      String[] aliases = command.getAliases();
      for (int i = 0; i < aliases.length; i++) {
        if (!execute.equalsIgnoreCase(aliases[i].replaceAll(" ", ""))) continue;
        try {
          return command.dispatch(arguments);
        } catch (Exception e) {
          return prefix + aliases[i] + " " + command.getSyntax();
        }
      }
    }
    return "Unknown command. Try " + prefix + "help.";
  }

  /** Dispatches a chat message, printing the response to chat. Returns true if handled. */
  public boolean dispatch(String message) {
    if (message == null || !message.trim().startsWith(prefix)) {
      return false;
    }
    String response = dispatchDirect(message);
    if (response != null && !response.isEmpty()) {
      Logger.getLogger().printToChat(response.replace("&", "\u00a7"));
    }
    return true;
  }
}
