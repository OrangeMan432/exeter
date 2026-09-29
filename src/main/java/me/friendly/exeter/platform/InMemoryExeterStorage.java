package me.friendly.exeter.platform;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link ExeterStorage} that keeps everything in memory for the lifetime of the process.
 *
 * <p>This is what a browser build uses until the host installs a persistent backend, so the client
 * is fully usable (modules, settings, ClickGUI) without a filesystem. Values are lost on reload; a
 * host that wants persistence installs a real implementation during bootstrap.
 */
public final class InMemoryExeterStorage implements ExeterStorage {

  private final Map<String, String> entries = new ConcurrentHashMap<>();

  @Override
  public String read(String name) {
    return entries.get(name);
  }

  @Override
  public void write(String name, String contents) {
    if (contents == null) {
      entries.remove(name);
    } else {
      entries.put(name, contents);
    }
  }

  @Override
  public void delete(String name) {
    entries.remove(name);
  }

  /** Drops every stored value. Used by tests and by "reset config" style actions. */
  public void clear() {
    entries.clear();
  }
}
