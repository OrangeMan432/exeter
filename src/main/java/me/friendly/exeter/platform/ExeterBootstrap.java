package me.friendly.exeter.platform;

import me.friendly.exeter.core.Exeter;

/**
 * Host-agnostic entry point for starting the client.
 *
 * <p>Every host starts Exeter the same way, through {@link #init()}. What differs between hosts is
 * only the platform wiring installed beforehand: the Java Edition entry point installs a
 * file-backed {@link ExeterStorage} and a mixin-backed {@link UserSwapper}, while an Eaglercraft
 * build installs a browser-storage-backed store and leaves session swapping as a no-op.
 *
 * <p>Keep this class free of host-specific imports so both builds can call it. The Fabric {@code
 * ModInitializer} lives in the host source set, not in core.
 */
public final class ExeterBootstrap {

  private static volatile UserSwapper userSwapper = UserSwapper.NOOP;
  private static volatile boolean initialized;

  private ExeterBootstrap() {}

  /** Installs the session-swapping strategy. Must be called before {@link #init()}. */
  public static void setUserSwapper(UserSwapper swapper) {
    userSwapper = swapper == null ? UserSwapper.NOOP : swapper;
  }

  /** Returns the active session-swapping strategy, never null. */
  public static UserSwapper userSwapper() {
    return userSwapper;
  }

  /**
   * Starts the client. Safe to call more than once; later calls are ignored.
   *
   * <p>Install any platform wiring (storage backend, user swapper) before calling this, because
   * loading the config and the debug log happens during startup.
   */
  public static synchronized void init() {
    if (initialized) {
      return;
    }
    initialized = true;
    new Exeter();
  }

  /** Whether {@link #init()} has already run. */
  public static boolean isInitialized() {
    return initialized;
  }
}
