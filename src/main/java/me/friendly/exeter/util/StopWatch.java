package me.friendly.exeter.util;

public class StopWatch {
  private long lastMs;

  public StopWatch() {
    lastMs = System.currentTimeMillis();
  }

  public boolean hasPassed(long ms) {
    return System.currentTimeMillis() - lastMs >= ms;
  }

  public boolean hasPassed(int ms) {
    return hasPassed((long) ms);
  }

  public void reset() {
    lastMs = System.currentTimeMillis();
  }

  public long getElapsed() {
    return System.currentTimeMillis() - lastMs;
  }
}
