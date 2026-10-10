package me.friendly.exeter.plugin;

import java.io.*;
import java.io.File;
import java.net.*;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import me.friendly.exeter.core.Exeter;

public class PluginManager extends ListManager<Plugin> implements PluginManagerImpl {
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
    return loaded;
  }

  /**
   * Loads one jar, instantiating every direct {@link Plugin} subclass inside. Already-loaded plugin
   * names are skipped.
   */
  public Optional<Plugin> loadFile(File file) {
    if (!file.isFile() || !file.getName().endsWith(".jar")) return Optional.empty();
    Plugin found = null;
    try (JarFile jarFile = new JarFile(file)) {
      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();
        if (!name.endsWith(".class")) continue;
        String className = name.replace('/', '.');
        className = className.substring(0, className.length() - 6);
        URLClassLoader classLoader =
            new URLClassLoader(
                new URL[] {new URL("file:///" + file.getAbsolutePath())},
                getClass().getClassLoader());
        Class<?> clazz = classLoader.loadClass(className);
        if (clazz == null
            || clazz.getSuperclass() == null
            || !clazz.getSuperclass().equals(Plugin.class)) continue;
        Plugin plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();
        if (get(plugin.getName()).isPresent()) continue;
        this.getList().add(plugin);
        plugin.setRunning(true);
        found = plugin;
      }
    } catch (IOException
        | ClassNotFoundException
        | IllegalAccessException
        | InstantiationException
        | NoSuchMethodException
        | java.lang.reflect.InvocationTargetException e) {
      e.printStackTrace();
    }
    return Optional.ofNullable(found);
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

  /**
   * @return if an update is needed or not.
   */
  @Override
  public boolean needsUpdate() {
    return false;
  }
}
