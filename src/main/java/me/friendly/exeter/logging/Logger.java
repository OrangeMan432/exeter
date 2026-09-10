package me.friendly.exeter.logging;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class Logger {
  /** Logger instance */
  private static Logger logger = null;

  /**
   * Appends message param to client tag, and prints it to the log.
   *
   * @param message to be printed
   */
  public void print(String message) {
    System.out.println(String.format("[%s] %s", "Skid Client", message));
  }

  /**
   * Appends message param to client tag, and prints it to the chat.
   *
   * @param message to be printed
   */
  public void printToChat(String message) {
    Minecraft.getInstance()
        .player
        .sendSystemMessage(
            Component.literal(String.format("§c[%s] §7%s", "Skid Client", message.replace("&", "§")))
                .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
  }

  public static Logger getLogger() {
    return logger == null ? (logger = new Logger()) : logger;
  }
}
