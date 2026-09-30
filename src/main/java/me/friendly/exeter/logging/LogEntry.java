package me.friendly.exeter.logging;

public final class LogEntry {
  public enum Level {
    INFO,
    WARN,
    ERROR,
    OUTPUT
  }

  private final String message;
  private final Level level;
  private final long timestamp;

  public LogEntry(String message, Level level) {
    this.message = message;
    this.level = level;
    this.timestamp = System.currentTimeMillis();
  }

  public String getMessage() {
    return message;
  }

  public Level getLevel() {
    return level;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
