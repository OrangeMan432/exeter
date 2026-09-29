package me.friendly.exeter.platform;

import me.friendly.exeter.core.Exeter;

/**
 * Host-agnostic entry point for starting the client.
 *
 * <p>Every host starts Exeter the same way, through {@link #init()}. What differs between hosts is
 * only the storage backend installed beforehand: the Java Edition entry point installs a
 * file-backed {@link ExeterStorage}, while an Eaglercraft build installs a browser-storage-backed
 * store or leaves the in-memory default.
 *
 * <p>Keep this class free of host-specific imports so both builds can call it. The Fabric {@code
 * ModInitializer} lives in the host source set, not in core.
 */
public final class ExeterBootstrap {

  private static volatile boolean initialized;
  private static volatile Runnable postInit = () -> {};

  private ExeterBootstrap() {}

  /**
   * Installs work to run once the client has finished initializing. Hosts use this to attach a test
   * or diagnostic harness, since core cannot depend on one. Call before {@link #init()}.
   */
  public static void setPostInitHook(Runnable hook) {
    postInit = hook == null ? () -> {} : hook;
  }

  /** Runs the post-init hook, if any. Called by the client once startup finishes. */
  public static void runPostInit() {
    postInit.run();
  }

  /**
   * Writes every registered config back to storage.
   *
   * <p>Hosts call this when the game is going away. A desktop JVM does it from a shutdown hook; a
   * browser tab has no such event, so an Eaglercraft build calls it when the window closes or the
   * player leaves the world.
   */
  public static void saveAll() {
    Exeter exeter = Exeter.getInstance();
    if (exeter != null) {
      exeter.saveAll();
    }
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
