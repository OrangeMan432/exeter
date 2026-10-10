package me.friendly.exeter.plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.io.File;
import java.net.*;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import me.friendly.exeter.core.Exeter;

public class PluginManager extends ListManager<Plugin> implements PluginManagerImpl {
  /** Lowercase plugin name to the jar it was loaded from, so removal can delete it. */
  private final Map<String, File> jarByPlugin = new HashMap<>();

  /** Display metadata from exeter-plugin.json, keyed the same way. */
  private final Map<String, PluginInfo> infoByPlugin = new HashMap<>();

  /** Modules a plugin registered while starting, removed again on uninstall. */
  private final Map<String, java.util.List<me.friendly.exeter.module.Module>> modulesByPlugin =
      new HashMap<>();

  /** Commands a plugin registered while starting, removed again on uninstall. */
  private final Map<String, java.util.List<me.friendly.exeter.command.Command>> commandsByPlugin =
      new HashMap<>();

  public PluginManager() {
    // Copy-on-write: installs complete on worker threads while the render
    // thread iterates this list for windows and HUDs.
    super(new java.util.concurrent.CopyOnWriteArrayList<>());
  }

  /**
   * Scans the plugin list for Plugins that match the name param given. It then returns optional
   * with the plugin
   *
   * @param name the name of the plugin.
   */
  @Override
  public Optional<Plugin> get(String name) {
    for (Plugin plugin : getList()) {
      if (!plugin.getName().equalsIgnoreCase(name)) continue;
      return Optional.of(plugin);
    }
    return Optional.empty();
  }

  /**
   * @return the Plugin directory
   */
  @Override
  public File getFile() {
    return new File(Exeter.getInstance().getDirectory(), "plugins");
  }

  /**
   * Called on load.
   *
   * <p>Loads all found Plugins.
   */
  @Override
  public void onLoad() throws IOException {
    ensureDir();
    for (File file : jarFiles()) {
      loadFile(file);
    }
  }

  /**
   * Picks up jars added after startup (e.g. downloaded through the plugin manager). Jars whose
   * plugin name is already loaded are skipped: classes cannot be unloaded, so updating a plugin
   * still needs a restart.
   *
   * @return newly loaded plugins.
   */
  public java.util.List<Plugin> reload() {
    ensureDir();
    java.util.List<Plugin> loaded = new java.util.ArrayList<>();
    for (File file : jarFiles()) {
      loadFile(file).ifPresent(loaded::add);
    }
    debug("reload: " + loaded.size() + " new plugin(s)");
    return loaded;
  }

  /**
   * Loads one jar, instantiating every direct {@link Plugin} subclass inside. Already-loaded plugin
   * names are skipped. List mutation and starting hop to the client thread when called off-thread,
   * since both touch lists the render thread iterates.
   */
  public Optional<Plugin> loadFile(File file) {
    if (!file.isFile() || !file.getName().endsWith(".jar")) return Optional.empty();
    Plugin found = null;
    boolean sawDuplicate = false;
    PluginInfo declared = null;
    java.util.List<Plugin> fresh = new java.util.ArrayList<>();
    // One loader per jar, closed after the scan: an open loader locks the file
    // on Windows and the jar could never be deleted on uninstall.
    try (JarFile jarFile = new JarFile(file);
        URLClassLoader classLoader =
            new URLClassLoader(
                new URL[] {new URL("file:///" + file.getAbsolutePath())},
                getClass().getClassLoader())) {
      declared = readInfo(jarFile, file.getName());
      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();
        if (!name.endsWith(".class")) continue;
        String className = name.replace('/', '.');
        className = className.substring(0, className.length() - 6);
        Class<?> clazz = classLoader.loadClass(className);
        if (clazz == null
            || clazz.getSuperclass() == null
            || !clazz.getSuperclass().equals(Plugin.class)) continue;
        Plugin plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();
        if (get(plugin.getName()).isPresent()) {
          debug(file.getName() + ": " + plugin.getName() + " already loaded, skipping");
          sawDuplicate = true;
          continue;
        }
        fresh.add(plugin);
        if (found == null) found = plugin;
      }
    } catch (IOException
        | ClassNotFoundException
        | IllegalAccessException
        | InstantiationException
        | NoSuchMethodException
        | java.lang.reflect.InvocationTargetException e) {
      e.printStackTrace();
      debug(file.getName() + ": load failed: " + e.getMessage());
    }
    if (!fresh.isEmpty()) {
      PluginInfo info = declared;
      Runnable start =
          () -> {
            var moduleManager = Exeter.getInstance().getModuleManager();
            var commandManager = Exeter.getInstance().getCommandManager();
            for (Plugin plugin : fresh) {
              if (get(plugin.getName()).isPresent()) continue;
              java.util.Set<me.friendly.exeter.module.Module> beforeModules =
                  new java.util.HashSet<>(moduleManager.getRegistry());
              java.util.Set<me.friendly.exeter.command.Command> beforeCommands =
                  new java.util.HashSet<>(commandManager.getRegistry());
              this.getList().add(plugin);
              jarByPlugin.put(plugin.getName().toLowerCase(), file);
              infoByPlugin.put(
                  plugin.getName().toLowerCase(),
                  info == null ? PluginInfo.unknown(plugin.getName()) : info);
              plugin.setRunning(true);
              trackOwned(plugin, moduleManager, commandManager, beforeModules, beforeCommands);
              debug(file.getName() + ": loaded " + plugin.getName());
            }
          };
      // Null during startup init (server-side entrypoint): that thread is the
      // game thread by construction, so starting directly is safe.
      net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
      if (minecraft == null || minecraft.isSameThread()) {
        start.run();
      } else {
        minecraft.execute(start);
      }
    } else if (!sawDuplicate) {
      debug(file.getName() + ": no Plugin subclass inside");
    }
    return Optional.ofNullable(found);
  }

  /** Reads exeter-plugin.json from the jar root; null when absent or invalid. */
  private PluginInfo readInfo(JarFile jarFile, String fileName) {
    JarEntry entry = jarFile.getJarEntry("exeter-plugin.json");
    if (entry == null) return null;
    try (var in = jarFile.getInputStream(entry);
        var reader = new java.io.InputStreamReader(in, StandardCharsets.UTF_8)) {
      JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
      return new PluginInfo(string(json, "name"), string(json, "author"), string(json, "version"));
    } catch (Exception e) {
      debug(fileName + ": bad exeter-plugin.json: " + e.getMessage());
      return null;
    }
  }

  private static String string(JsonObject json, String key) {
    return json.has(key) ? json.get(key).getAsString() : "?";
  }

  /**
   * Starts a plugin on the client thread: start hooks register modules and open windows, and those
   * lists are iterated by the render thread.
   */
  private void debug(String message) {
    me.friendly.exeter.logging.DebugLogger.get().logSystem("Plugins", message);
  }

  private void ensureDir() {
    if (!this.getFile().exists()) {
      this.getFile().mkdirs();
      this.getFile().mkdir();
    }
  }

  private File[] jarFiles() {
    File[] files = this.getFile().listFiles();
    return files == null ? new File[0] : files;
  }

  /** Metadata from the jar, or unknown placeholders when it ships none. */
  public PluginInfo info(String name) {
    PluginInfo info = infoByPlugin.get(name.toLowerCase());
    return info == null ? PluginInfo.unknown(name) : info;
  }

  /**
   * Remembers what a plugin registered while starting (all starts run on the client thread, so the
   * diff is exact) for removal on uninstall.
   */
  private void trackOwned(
      Plugin plugin,
      me.friendly.exeter.module.ModuleManager moduleManager,
      me.friendly.exeter.command.CommandManager commandManager,
      java.util.Set<me.friendly.exeter.module.Module> beforeModules,
      java.util.Set<me.friendly.exeter.command.Command> beforeCommands) {
    java.util.List<me.friendly.exeter.module.Module> ownedModules = new java.util.ArrayList<>();
    for (me.friendly.exeter.module.Module module : moduleManager.getRegistry()) {
      if (!beforeModules.contains(module)) ownedModules.add(module);
    }
    modulesByPlugin.put(plugin.getName().toLowerCase(), ownedModules);
    java.util.List<me.friendly.exeter.command.Command> ownedCommands = new java.util.ArrayList<>();
    for (me.friendly.exeter.command.Command command : commandManager.getRegistry()) {
      if (!beforeCommands.contains(command)) ownedCommands.add(command);
    }
    commandsByPlugin.put(plugin.getName().toLowerCase(), ownedCommands);
  }

  /**
   * Stops a plugin, drops it from the list and deletes its jar so it stays gone across restarts.
   * Classes cannot unload, so the jar must go.
   *
   * @return false when no loaded plugin has that name.
   */
  public boolean uninstall(String name) {
    Optional<Plugin> existing = get(name);
    if (existing.isEmpty()) return false;
    Plugin plugin = existing.get();
    plugin.setRunning(false);
    this.getList().remove(plugin);
    infoByPlugin.remove(name.toLowerCase());
    var moduleManager = Exeter.getInstance().getModuleManager();
    java.util.List<me.friendly.exeter.module.Module> ownedModules =
        modulesByPlugin.remove(name.toLowerCase());
    if (ownedModules != null) {
      for (me.friendly.exeter.module.Module module : ownedModules) {
        if (module instanceof me.friendly.exeter.module.ToggleableModule toggleable
            && toggleable.isRunning()) {
          toggleable.setRunning(false);
        }
        moduleManager.unregister(module);
        debug(name + ": unregistered module " + module.getLabel());
      }
    }
    var commandManager = Exeter.getInstance().getCommandManager();
    java.util.List<me.friendly.exeter.command.Command> ownedCommands =
        commandsByPlugin.remove(name.toLowerCase());
    if (ownedCommands != null) {
      for (me.friendly.exeter.command.Command command : ownedCommands) {
        commandManager.unregister(command);
      }
    }
    File jar = jarByPlugin.remove(name.toLowerCase());
    if (jar != null && jar.exists() && !jar.delete()) {
      debug(name + ": stopped but the jar would not delete");
      return true;
    }
    debug(name + ": uninstalled");
    return true;
  }

  /**
   * @return if an update is needed or not.
   */
  @Override
  public boolean needsUpdate() {
    return false;
  }
}
