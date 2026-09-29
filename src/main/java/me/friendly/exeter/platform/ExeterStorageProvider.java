package me.friendly.exeter.platform;

/**
 * Holds the active {@link ExeterStorage} backend.
 *
 * <p>Core code never picks a backend itself. A host installs one during bootstrap (the Java Edition
 * entry point installs a file-backed store under the game's config directory; an Eaglercraft build
 * installs a browser-storage-backed store or leaves the in-memory default). Reads before any
 * install fall back to {@link InMemoryExeterStorage} so nothing needs null checks.
 */
public final class ExeterStorageProvider {

  private static volatile ExeterStorage storage = new InMemoryExeterStorage();

  private ExeterStorageProvider() {}

  /** Returns the active backend, never null. */
  public static ExeterStorage get() {
    return storage;
  }

  /** Installs {@code backend} as the active store. Passing null restores the in-memory default. */
  public static void set(ExeterStorage backend) {
    storage = backend == null ? new InMemoryExeterStorage() : backend;
  }
}
