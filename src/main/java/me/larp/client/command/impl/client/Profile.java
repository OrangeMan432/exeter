package me.larp.client.command.impl.client;

import java.util.List;
import me.larp.client.command.Argument;
import me.larp.client.command.Command;
import me.larp.client.config.ProfileManager;

/** Raven-style profiles: .profile save/load/list/delete/5b5t */
public final class Profile extends Command {
  public Profile() {
    super(new String[] {"profile", "profiles", "config"}, new Argument("action"), new Argument("name"));
  }

  @Override
  public String dispatch() {
    String action = this.getArgument("action").getValue();
    if (action == null) return "Usage: .profile <save|load|list|delete|5b5t> [name]";
    switch (action.toLowerCase()) {
      case "list" -> {
        List<String> names = ProfileManager.list();
        return names.isEmpty()
            ? "No profiles saved. Use .profile save <name>."
            : "Profiles: " + String.join(", ", names);
      }
      case "5b5t" -> {
        ProfileManager.apply5b5t();
        return "Applied the &e5b5t&7 setup and saved it.";
      }
      default -> {}
    }
    String name = this.getArgument("name").getValue();
    if (name == null) return "Usage: .profile <save|load|delete> <name>";
    switch (action.toLowerCase()) {
      case "save" -> {
        return ProfileManager.save(name)
            ? "Saved profile &e" + name + "&7."
            : "Failed to save profile &e" + name + "&7.";
      }
      case "load" -> {
        if (name.equalsIgnoreCase("5b5t") && !ProfileManager.list().contains("5b5t")) {
          ProfileManager.apply5b5t();
          return "Applied the &e5b5t&7 setup and saved it.";
        }
        return ProfileManager.load(name)
            ? "Loaded profile &e" + name + "&7."
            : "No such profile: &e" + name + "&7. Saved: "
                + String.join(", ", ProfileManager.list());
      }
      case "delete" -> {
        return ProfileManager.delete(name)
            ? "Deleted profile &e" + name + "&7."
            : "Cannot delete &e" + name + "&7.";
      }
      default -> {
        return "Usage: .profile <save|load|list|delete|5b5t> [name]";
      }
    }
  }
}
