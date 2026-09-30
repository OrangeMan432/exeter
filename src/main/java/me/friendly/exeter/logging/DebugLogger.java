package me.friendly.exeter.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.util.NotificationManager;

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
  private final Map<String, Boolean> moduleEnabled = new ConcurrentHashMap<String, Boolean>();
  private volatile boolean logToFile = false;
  private volatile boolean logToChat = false;
  private volatile boolean logToNotifications = false;
  private volatile boolean enabled = false;
  private volatile boolean showInfo = true;
  private volatile boolean showWarn = true;
  private volatile boolean showError = true;
  private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

  private DebugLogger() {}

  public static DebugLogger get() {
    if (instance == null) {
      instance = new DebugLogger();
    }
    return instance;
  }

  private File logFile() {
    File dir = new File("exeter");
    if (!dir.exists()) {
      dir.mkdirs();
    }
    return new File(dir, "debug.log");
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

  public void setLogToChat(boolean logToChat) {
    this.logToChat = logToChat;
  }

  public void setLogToNotifications(boolean logToNotifications) {
    this.logToNotifications = logToNotifications;
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

  public void setModuleEnabled(String moduleName, boolean enabled) {
    moduleEnabled.put(moduleName, Boolean.valueOf(enabled));
  }

  public boolean isModuleEnabled(String moduleName) {
    Boolean value = moduleEnabled.get(moduleName);
    return value != null && value.booleanValue();
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
    if (level == Level.INFO && !showInfo) return;
    if (level == Level.WARN && !showWarn) return;
    if (level == Level.ERROR && !showError) return;

    String prefix = "";
    if (level == Level.WARN) prefix = "[WARNING] ";
    if (level == Level.ERROR) prefix = "[ERROR] ";
    String timestamp = LocalDateTime.now().format(timeFmt);
    String formatted = "[" + timestamp + "] [" + module + "] " + prefix + message;

    if (logToFile) {
      writeToFile(formatted);
    }

    Logger.getLogger().dispatch(new LogEntry(formatted, toEntryLevel(level)));

    if (logToChat) {
      sendToChat(formatted);
    }

    if (logToNotifications) {
      String icon = "info";
      if (level == Level.WARN) icon = "warning";
      if (level == Level.ERROR) icon = "error";
      NotificationManager.push("[" + module + "] " + prefix + message, icon);
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
      NotificationManager.push("[" + tag + "] " + message, "info");
    }
  }

  /**
   * Writes a timestamped line to the debug log file only. Unlike {@link #log}, this never touches
   * chat or notifications, for detail that is only readable in the file.
   *
   * @param tag a tag for the source (e.g. a module name)
   * @param message the debug message
   */
  public void logFile(String tag, String message) {
    if (!logToFile) return;

    String timestamp = LocalDateTime.now().format(timeFmt);
    String formatted = "[" + timestamp + "] [" + tag + "] " + message;
    writeToFile(formatted);
  }

  /** Clears the debug log file. */
  public void clearLog() {
    try {
      PrintWriter pw = new PrintWriter(logFile());
      try {
        pw.print("");
      } finally {
        pw.close();
      }
    } catch (Exception e) {
      System.err.println("[Exeter Debug] Failed to clear log: " + e.getMessage());
    }
  }

  private void writeToFile(String message) {
    try {
      FileWriter writer = new FileWriter(logFile(), true);
      try {
        PrintWriter pw = new PrintWriter(writer);
        pw.println(message);
        pw.flush();
      } finally {
        writer.close();
      }
    } catch (Exception e) {
      System.err.println("[Exeter Debug] Failed to write log: " + e.getMessage());
    }
  }

  private void sendToChat(String message) {
    try {
      if (Exeter.getInstance() == null) return;
      Logger.getLogger().printToChat(message);
    } catch (Exception ignored) {
    }
  }

  private LogEntry.Level toEntryLevel(Level level) {
    if (level == Level.WARN) return LogEntry.Level.WARN;
    if (level == Level.ERROR) return LogEntry.Level.ERROR;
    return LogEntry.Level.INFO;
  }
}
