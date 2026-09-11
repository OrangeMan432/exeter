package me.larp.client.command.impl.client;

import me.larp.client.command.Argument;
import me.larp.client.command.Command;
import me.larp.client.module.impl.toggle.misc.Spammer;

/** .spam <message...> sets the Spammer text. */
public final class Spam extends Command {
  public Spam() {
    super(new String[] {"spam"}, new Argument("message"));
  }

  @Override
  public String dispatch(String[] input) {
    if (input.length < 2) return "Usage: .spam <message>";
    StringBuilder text = new StringBuilder();
    for (int i = 1; i < input.length; i++) {
      if (i > 1) text.append(" ");
      text.append(input[i]);
    }
    Spammer.setMessage(text.toString());
    return "Spammer message set.";
  }
}
