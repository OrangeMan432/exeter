package me.friendly.exeter.config;

import me.friendly.api.interfaces.Labeled;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.platform.ExeterStorageProvider;

/**
 * An object whose state is persisted under a single storage key, and an implementation of Labeled.
 *
 * <p>Contents go through {@link ExeterStorageProvider} rather than {@code java.io.File}, so the
 * same subclasses work on a desktop JVM and in a browser build that has no filesystem. Subclasses
 * read and write the whole value with {@link #read()} and {@link #write(String)}; line-oriented
 * formats are the caller's business.
 */
public abstract class Config implements Labeled {
  private final String label;

  /**
   * Instantiates the label and registers this instance with the ConfigManager.
   *
   * @param label the storage key this config is saved under
   */
  public Config(String label) {
    this.label = label;
    Exeter.getInstance().getConfigManager().register(this);
  }

  @Override
  public String getLabel() {
    return this.label;
  }

  /** Returns the stored contents, or null when this config has never been saved. */
  protected String read() {
    return ExeterStorageProvider.get().read(this.label);
  }

  /** Whether this config has a stored value. */
  protected boolean exists() {
    return read() != null;
  }

  /** Stores {@code contents} as this config's value. */
  protected void write(String contents) {
    ExeterStorageProvider.get().write(this.label, contents);
  }

  /** Removes the stored value. */
  protected void delete() {
    ExeterStorageProvider.get().delete(this.label);
  }

  public abstract void load(Object... var1);

  public abstract void save(Object... var1);
}
