package me.friendly.exeter.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Centralized debug logging system. Modules call {@link #log(String, String)} to log messages.
 * Messages are routed to file, chat, or both based on the Debug module's settings.
 */
public final class DebugLogger {
  public enum Level {
    INFO,
    WARN,
    ERROR
  }

  private static DebugLogger instance;
  private final File logFile;
  private final Map<String, Boolean> moduleEnabled = new ConcurrentHashMap<>();
  private volatile boolean logToFile = false;
  private volatile boolean logToChat = false;
  private volatile boolean logToNotifications = false;
  private volatile boolean enabled = false;
  private volatile boolean showInfo = true;
  private volatile boolean showWarn = true;
  private volatile boolean showError = true;
  private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

  private DebugLogger() {
    this.logFile =
        FabricLoader.getInstance().getConfigDir().resolve("exeter").resolve("debug.log").toFile();
    FabricLoader.getInstance().getConfigDir().resolve("exeter").toFile().mkdirs();
  }

  public static DebugLogger get() {
    if (instance == null) {
      instance = new DebugLogger();
    }
    return instance;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setLogToFile(boolean logToFile) {
    this.logToFile = logToFile;
  }

  public boolean isLogToFile() {
    return logToFile;
  }

  public void setLogToChat(boolean logToChat) {
    this.logToChat = logToChat;
  }

  public boolean isLogToChat() {
    return logToChat;
  }

  public void setLogToNotifications(boolean logToNotifications) {
    this.logToNotifications = logToNotifications;
  }

  public boolean isLogToNotifications() {
    return logToNotifications;
  }

  public void setShowInfo(boolean v) {
    showInfo = v;
  }

  public void setShowWarn(boolean v) {
    showWarn = v;
  }

  public void setShowError(boolean v) {
    showError = v;
  }

  public boolean isLevelEnabled(Level level) {
    return switch (level) {
      case INFO -> showInfo;
      case WARN -> showWarn;
      case ERROR -> showError;
    };
  }

  public void setModuleEnabled(String moduleName, boolean enabled) {
    moduleEnabled.put(moduleName, enabled);
  }

  public boolean isModuleEnabled(String moduleName) {
    return moduleEnabled.getOrDefault(moduleName, false);
  }

  /**
   * Logs a debug message from a module. If the module is enabled for debugging, the message is sent
   * to the configured outputs (file, chat, or both).
   *
   * @param module the module name (e.g. "AutoCart", "BedAura", "Config")
   * @param message the debug message
   */
  public void log(String module, String message) {
    log(module, Level.INFO, message);
  }

  public void log(String module, Level level, String message) {
    if (!enabled) return;
    if (!isModuleEnabled(module)) return;
    if (!isLevelEnabled(level)) return;

    String prefix = switch (level) {
      case INFO -> "";
      case WARN -> "[WARNING] ";
      case ERROR -> "[ERROR] ";
    };
    String timestamp = LocalDateTime.now().format(timeFmt);
    String formatted = "[" + timestamp + "] [" + module + "] " + prefix + message;

    if (logToFile) {
      writeToFile(formatted);
    }

    LogEntry.Level entryLevel = switch (level) {
      case INFO -> LogEntry.Level.INFO;
      case WARN -> LogEntry.Level.WARN;
      case ERROR -> LogEntry.Level.ERROR;
    };
    Logger.getLogger().dispatch(new LogEntry(formatted, entryLevel));

    if (logToChat) {
      sendToChat(formatted);
    }

    if (logToNotifications) {
      String icon = switch (level) {
        case INFO -> "info";
        case WARN -> "warning";
        case ERROR -> "error";
      };
      sendToNotifications("[" + module + "] " + prefix + message, icon);
    }
  }

  /**
   * Logs a debug message from a module without requiring the module to be enabled. Only outputs if
   * global debug is enabled. Useful for system-level logging (e.g. config).
   *
   * @param tag a tag for the source (e.g. "Config", "Init")
   * @param message the debug message
   */
  public void logSystem(String tag, String message) {
    if (!enabled) return;

    String timestamp = LocalDateTime.now().format(timeFmt);
    String formatted = "[" + timestamp + "] [" + tag + "] " + message;

    Logger.getLogger().dispatch(new LogEntry(formatted, LogEntry.Level.INFO));

    if (logToFile) {
      writeToFile(formatted);
    }

    if (logToChat) {
      sendToChat(formatted);
    }

    if (logToNotifications) {
      sendToNotifications("[" + tag + "] " + message);
    }
  }

  /**
   * Logs a debug message regardless of per-module enable state. Only respects global enable and
   * output settings. Useful for config save/load where per-module state may not yet be loaded.
   *
   * @param tag a tag for the source
   * @param message the debug message
   */
  public void logAlways(String tag, String message) {
    if (!enabled) return;

    String timestamp = LocalDateTime.now().format(timeFmt);
    String formatted = "[" + timestamp + "] [" + tag + "] " + message;

    Logger.getLogger().dispatch(new LogEntry(formatted, LogEntry.Level.INFO));

    if (logToFile) {
      writeToFile(formatted);
    }

    if (logToChat) {
      sendToChat(formatted);
    }

    if (logToNotifications) {
      sendToNotifications("[" + tag + "] " + message);
    }
  }

  /** Clears the debug log file. */
  public void clearLog() {
    try (PrintWriter pw = new PrintWriter(logFile)) {
      pw.print("");
    } catch (Exception e) {
      System.err.println("[Exeter Debug] Failed to clear log: " + e.getMessage());
    }
  }

  private void writeToFile(String message) {
    try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
      pw.println(message);
    } catch (Exception e) {
      System.err.println("[Exeter Debug] Failed to write log: " + e.getMessage());
    }
  }

  private void sendToChat(String message) {
    try {
      net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
      if (mc.player != null) {
        mc.player.sendSystemMessage(
            net.minecraft.network.chat.Component.literal(
                "\u00a77[\u00a7cExeter Debug\u00a77] \u00a7f" + message));
      }
    } catch (Exception e) {
      // player not available, ignore
    }
  }

  private void sendToNotifications(String message, String icon) {
    try {
      me.friendly.exeter.util.NotificationManager.push(message, icon);
    } catch (Exception ignored) {}
  }

  private void sendToNotifications(String message) {
    sendToNotifications(message, "info");
  }
}
