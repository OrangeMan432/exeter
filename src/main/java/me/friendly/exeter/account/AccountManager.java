package me.friendly.exeter.account;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import me.friendly.api.event.Listener;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.config.Config;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.logging.DebugLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;

/**
 * Stores offline usernames, persists them to {@code accounts.json}, and tracks Hypixel ban state.
 *
 * <p>The Microsoft and token login paths were removed: they need outbound HTTPS to
 * login.microsoftonline.com and Mojang, whose responses carry no CORS headers, so a browser build
 * cannot complete them. Ban tracking is unaffected, because it is derived from disconnect packets
 * rather than from an auth call.
 *
 * <p>Ported from OpenMyau's {@code me.ksyz.accountmanager.AccountManager} and {@code Events}
 * (derived from https://github.com/ksyzov/AccountManager, originally LGPL, modified version GPL
 * v3). Forge GUI hooks were replaced with a {@link PacketEvent} listener: disconnect reasons are
 * scanned for ban messages, and joining Hypixel clears the ban flag, mirroring the upstream
 * behavior.
 */
public final class AccountManager extends ListRegistry<Account> {
  private static final String TAG = "AccountManager";

  private final Listener<PacketEvent> packetListener =
      new Listener<PacketEvent>("account_manager_packets") {
        @Override
        public void call(PacketEvent event) {
          if (event.getPacket() instanceof ClientboundDisconnectPacket packet) {
            handleDisconnect(packet.reason());
          } else if (event.getPacket() instanceof ClientboundLoginDisconnectPacket packet) {
            handleDisconnect(packet.reason());
          } else if (event.getPacket() instanceof ClientboundLoginPacket) {
            handleJoin();
          }
        }
      };

  private final Config config;

  public AccountManager() {
    this.registry = new ArrayList<>();
    this.config =
        new Config("accounts.json") {
          @Override
          public void load(Object... source) {
            String contents = this.read();
            if (contents == null || contents.isBlank()) {
              return;
            }
            JsonElement root;
            try {
              root = JsonParser.parseString(contents);
            } catch (Exception e) {
              DebugLogger.get().log(TAG, DebugLogger.Level.ERROR, "Couldn't read accounts.json");
              return;
            }
            if (!(root instanceof JsonArray)) {
              return;
            }
            AccountManager.this.getRegistry().clear();
            for (JsonElement element : (JsonArray) root) {
              if (!(element instanceof JsonObject object)) {
                continue;
              }
              try {
                AccountManager.this.register(
                    new Account(
                        getString(object, "username"),
                        getString(object, "uuid"),
                        getLong(object, "unban")));
              } catch (Exception e) {
                DebugLogger.get()
                    .log(TAG, DebugLogger.Level.WARN, "Skipping malformed account entry");
              }
            }
            DebugLogger.get()
                .logFile(TAG, "Loaded " + AccountManager.this.getRegistry().size() + " accounts");
          }

          @Override
          public void save(Object... destination) {
            JsonArray accounts = new JsonArray();
            for (Account account : AccountManager.this.getRegistry()) {
              JsonObject object = new JsonObject();
              object.addProperty("username", account.getUsername());
              object.addProperty("uuid", account.getUuid());
              object.addProperty("unban", account.getUnban());
              accounts.add(object);
            }
            try {
              this.write(new GsonBuilder().setPrettyPrinting().create().toJson(accounts));
            } catch (Exception e) {
              DebugLogger.get().log(TAG, DebugLogger.Level.ERROR, "Couldn't save accounts.json");
            }
          }
        };
    Exeter.getInstance().getEventManager().register(packetListener);
  }

  private void handleDisconnect(Component reason) {
    if (reason == null) {
      return;
    }
    String text = reason.getString();
    String firstLine = text.split("\n")[0].trim();
    Minecraft mc = Minecraft.getInstance();
    String current = mc.getUser() != null ? mc.getUser().getName() : "";

    if (firstLine.equals("You are permanently banned from this server!")
        || firstLine.equals("Your account has been blocked.")) {
      flagCurrentAccount(current, -1L);
      return;
    }

    String duration = null;
    if (firstLine.startsWith("You are temporarily banned for ")
        && firstLine.endsWith(" from this server!")) {
      duration =
          firstLine.substring(
              "You are temporarily banned for ".length(),
              firstLine.length() - " from this server!".length());
    } else if (firstLine.startsWith("Your account is temporarily blocked for ")) {
      String tail = " from this server!";
      if (firstLine.endsWith(tail)) {
        duration =
            firstLine.substring(
                "Your account is temporarily blocked for ".length(),
                firstLine.length() - tail.length());
      }
    }
    if (duration != null) {
      flagCurrentAccount(current, parseUnban(duration));
    }
  }

  /** Clears the ban flag when joining Hypixel, mirroring upstream world-load handling. */
  private void handleJoin() {
    ServerData server = Minecraft.getInstance().getCurrentServer();
    if (server == null || server.ip == null) {
      return;
    }
    String ip = server.ip.toLowerCase();
    if (!ip.endsWith("hypixel.net") && !ip.endsWith("hypixel.io")) {
      return;
    }
    String current = Minecraft.getInstance().getUser().getName();
    boolean changed = false;
    for (Account account : getRegistry()) {
      if (current.equals(account.getUsername()) && account.getUnban() != 0L) {
        account.setUnban(0L);
        changed = true;
      }
    }
    if (changed) {
      save();
    }
  }

  private void flagCurrentAccount(String username, long unban) {
    if (username == null || username.isEmpty()) {
      return;
    }
    boolean changed = false;
    for (Account account : getRegistry()) {
      if (username.equals(account.getUsername())) {
        account.setUnban(unban);
        changed = true;
      }
    }
    if (changed) {
      save();
      DebugLogger.get().log(TAG, DebugLogger.Level.WARN, "Flagged banned account: " + username);
    }
  }

  /** Parses durations like {@code "30d 1h 5m"} into an absolute unban timestamp. */
  private long parseUnban(String text) {
    long time = System.currentTimeMillis();
    for (String part : text.trim().split("\\s+")) {
      if (part.length() < 2) {
        continue;
      }
      try {
        long value = Long.parseLong(part.substring(0, part.length() - 1));
        switch (part.charAt(part.length() - 1)) {
          case 'd' -> time += value * 86400000L;
          case 'h' -> time += value * 3600000L;
          case 'm' -> time += value * 60000L;
          case 's' -> time += value * 1000L;
          default -> {}
        }
      } catch (NumberFormatException ignored) {
      }
    }
    return time;
  }

  private static String getString(JsonObject object, String key) {
    JsonElement element = object.get(key);
    return element != null && !element.isJsonNull() ? element.getAsString() : "";
  }

  private static long getLong(JsonObject object, String key) {
    try {
      JsonElement element = object.get(key);
      return element != null && !element.isJsonNull() ? element.getAsLong() : 0L;
    } catch (Exception e) {
      return 0L;
    }
  }

  public void save() {
    config.save(new Object[0]);
  }
}
