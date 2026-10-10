package me.friendly.exeter.command.impl.client;

import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.macro.Macro;
import me.friendly.exeter.macro.MacroManager;

/** Manage macros from chat; the Macros window does the same visually. */
public final class MacroCommand extends Command {
  public MacroCommand() {
    super(new String[] {"macro"}, new Argument("action"), new Argument("name"));
    setDescription("Run or flip a macro");
    addSubCommand("list", "", "List macros.");
    addSubCommand("run", "<name>", "Fire an instant macro.");
    addSubCommand("toggle", "<name>", "Flip a toggle macro.");
  }

  @Override
  public String dispatch(String[] input) {
    if (input.length == 2 && input[1].equalsIgnoreCase("list")) {
      MacroManager macros = Exeter.getInstance().getMacroManager();
      if (macros.getRegistry().isEmpty()) {
        return "No macros. Open the Macros window to create one.";
      }
      StringBuilder out = new StringBuilder();
      for (Macro macro : macros.getRegistry()) {
        if (out.length() > 0) out.append(", ");
        out.append(macro.getName());
      }
      return out.toString();
    }
    return super.dispatch(input);
  }

  @Override
  public String dispatch() {
    MacroManager macros = Exeter.getInstance().getMacroManager();
    String action = this.getArgument("action").getValue();
    if (action.equalsIgnoreCase("list")) {
      if (macros.getRegistry().isEmpty()) {
        return "No macros. Open the Macros window to create one.";
      }
      StringBuilder out = new StringBuilder();
      for (Macro macro : macros.getRegistry()) {
        if (out.length() > 0) out.append(", ");
        out.append(macro.getName());
      }
      return out.toString();
    }
    String name = this.getArgument("name").getValue();
    Macro macro = macros.getMacro(name);
    if (macro == null) {
      return "No such macro: " + name;
    }
    if (action.equalsIgnoreCase("run")) {
      if (macro.getMode() != Macro.Mode.INSTANT) {
        return "Not an instant macro, use toggle.";
      }
      macros.fire(macro);
      return "Fired " + macro.getName() + ".";
    }
    if (action.equalsIgnoreCase("toggle")
        || action.equalsIgnoreCase("enable")
        || action.equalsIgnoreCase("disable")) {
      if (macro.getMode() != Macro.Mode.TOGGLE) {
        return "Not a toggle macro, use run.";
      }
      boolean enable =
          action.equalsIgnoreCase("toggle")
              ? !macro.isEnabled()
              : action.equalsIgnoreCase("enable");
      macros.setEnabled(macro, enable);
      return macro.getName() + (macro.isEnabled() ? " enabled." : " disabled.");
    }
    return String.format("%s %s", "macro", this.getSyntax());
  }
}
