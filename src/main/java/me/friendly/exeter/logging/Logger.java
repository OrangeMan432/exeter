package me.friendly.exeter.logging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class Logger {
  private static Logger logger = null;
  private final List<Consumer<LogEntry>> listeners = new CopyOnWriteArrayList<>();

  public void print(String message) {
    String formatted = String.format("[%s] %s", "Exeter", message);
    System.out.println(formatted);
    dispatch(new LogEntry(formatted, LogEntry.Level.INFO));
  }

  public void printToChat(String message) {
    Minecraft.getInstance()
        .player
        .sendSystemMessage(
            Component.literal(String.format("§c[%s] §7%s", "Exeter", message.replace("&", "§")))
                .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
  }

  public void addListener(Consumer<LogEntry> listener) {
    listeners.add(listener);
  }

  public void removeListener(Consumer<LogEntry> listener) {
    listeners.remove(listener);
  }

  public void dispatch(LogEntry entry) {
    for (Consumer<LogEntry> listener : listeners) {
      try {
        listener.accept(entry);
      } catch (Exception ignored) {}
    }
  }

  public static Logger getLogger() {
    return logger == null ? (logger = new Logger()) : logger;
  }
}
