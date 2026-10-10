package me.friendly.exeter.command.impl.client;

import java.io.File;
import java.util.List;
import java.util.Optional;
import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.plugin.Plugin;
import me.friendly.exeter.plugin.PluginDownloader;
import me.friendly.exeter.plugin.PluginList;

/** Browse and install community plugins from the plugin list. */
public final class PluginCommand extends Command {
  public PluginCommand() {
    super(new String[] {"plugin"}, new Argument("action"), new Argument("name"));
    setDescription("Manage community plugins");
    addSubCommand("list", "", "List available plugins.");
    addSubCommand("installed", "", "List loaded plugins.");
    addSubCommand("install", "<name>", "Download and load a plugin.");
    addSubCommand("uninstall", "<name>", "Stop a plugin and delete its jar.");
    addSubCommand("reload", "", "Load jars added to the plugins folder.");
  }

  @Override
  public String dispatch(String[] input) {
    if (input.length == 2
        && (input[1].equalsIgnoreCase("list") || input[1].equalsIgnoreCase("installed"))) {
      return input[1].equalsIgnoreCase("list") ? listRemote() : listInstalled();
    }
    if (input.length == 2 && input[1].equalsIgnoreCase("reload")) {
      List<Plugin> loaded = Exeter.getInstance().getPluginManager().reload();
      if (loaded.isEmpty()) {
        return "No new plugins found.";
      }
      StringBuilder out = new StringBuilder("Loaded: ");
      for (int i = 0; i < loaded.size(); i++) {
        if (i > 0) out.append(", ");
        out.append(loaded.get(i).getName());
      }
      return out.toString();
    }
    return super.dispatch(input);
  }

  @Override
  public String dispatch() {
    String action = this.getArgument("action").getValue();
    if (action.equalsIgnoreCase("install")) {
      install(this.getArgument("name").getValue());
      return "Downloading...";
    }
    if (action.equalsIgnoreCase("uninstall")) {
      String name = this.getArgument("name").getValue();
      if (Exeter.getInstance().getPluginManager().uninstall(name)) {
        return "Uninstalled " + name + ".";
      }
      return "No loaded plugin: " + name;
    }
    return String.format("%s %s", "plugin", this.getSyntax());
  }

  private String listRemote() {
    List<PluginList.Entry> entries;
    try {
      entries = PluginList.fetch();
    } catch (Exception e) {
      return "Could not fetch the plugin list.";
    }
    if (entries.isEmpty()) {
      return "The plugin list is empty.";
    }
    StringBuilder out = new StringBuilder();
    for (PluginList.Entry entry : entries) {
      if (out.length() > 0) out.append(", ");
      out.append(entry.name()).append(" &8by ").append(entry.author()).append("&7");
    }
    return out.toString();
  }

  private String listInstalled() {
    List<Plugin> plugins = Exeter.getInstance().getPluginManager().getList();
    if (plugins.isEmpty()) {
      return "No plugins loaded.";
    }
    StringBuilder out = new StringBuilder();
    for (Plugin plugin : plugins) {
      if (out.length() > 0) out.append(", ");
      out.append(plugin.getName());
    }
    return out.toString();
  }

  private void install(String name) {
    new Thread(
            () -> {
              try {
                installSync(name);
              } catch (Exception e) {
                e.printStackTrace();
                print("Install failed: " + e.getMessage());
              }
            },
            "exeter-plugin-install")
        .start();
  }

  private void installSync(String name) {
    PluginList.Entry entry = findEntry(name);
    if (entry == null) {
      print("No such plugin in the list: " + name);
      return;
    }
    print("Downloading " + entry.name() + " " + entry.version() + "...");
    PluginDownloader.install(
        entry,
        Exeter.getInstance().getPluginManager().getFile(),
        (Optional<File> file) -> {
          if (file.isEmpty()) {
            print("Download failed for " + entry.name() + ".");
            return;
          }
          Optional<Plugin> plugin = Exeter.getInstance().getPluginManager().loadFile(file.get());
          if (plugin.isEmpty()) {
            print("Downloaded, but no plugin found inside " + file.get().getName() + ".");
            return;
          }
          print("Installed and loaded " + plugin.get().getName() + ".");
        });
  }

  private PluginList.Entry findEntry(String name) {
    try {
      List<PluginList.Entry> entries = PluginList.fetch();
      me.friendly.exeter.logging.DebugLogger.get()
          .logSystem("Plugins", "list fetched: " + entries.size() + " entries");
      for (PluginList.Entry entry : entries) {
        if (entry.name().equalsIgnoreCase(name)) return entry;
      }
    } catch (Exception e) {
      e.printStackTrace();
      me.friendly.exeter.logging.DebugLogger.get()
          .logSystem("Plugins", "list fetch failed: " + e.getMessage());
      return null;
    }
    return null;
  }

  /** Chat must print on the main thread; workers die silently otherwise. */
  private void print(String message) {
    minecraft.execute(() -> me.friendly.exeter.logging.Logger.getLogger().printToChat(message));
  }
}
