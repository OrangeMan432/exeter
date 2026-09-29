package me.friendly.exeter.platform;

/**
 * Persistent key/value storage for Exeter's config files, debug log and other small text blobs.
 *
 * <p>The client is built for two hosts: a normal JVM (Java Edition, where files live on disk under
 * the game's config directory) and Eaglercraft running in a browser (where there is no filesystem
 * and the backing store is whatever the host provides, typically browser storage). Core code only
 * ever talks to this interface so it stays portable across both.
 *
 * <p>Names are plain file names with no directory component, e.g. {@code clickgui.toml}.
 */
public interface ExeterStorage {

  /**
   * Returns the stored contents for {@code name}, or {@code null} when nothing is stored under that
   * name. An empty string is a valid stored value and is distinct from {@code null}.
   */
  String read(String name);

  /** Stores {@code contents} under {@code name}, replacing any previous value. */
  void write(String name, String contents);

  /** Removes {@code name}. Does nothing when it is not present. */
  default void delete(String name) {}
}
