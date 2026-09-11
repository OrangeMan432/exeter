package me.friendly.exeter.module.impl.toggle.client;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.status.ServerStatus;

public class DiscordRPC extends ToggleableModule {

  private static final long APP_ID = 1468971656407027888L;

  private RichPresence presence;
  private boolean started;
  private int tickCounter;

  public DiscordRPC() {
    super("DiscordRPC", new String[] {"discordrpc", "rpc"}, 0x5865F2, ModuleType.CLIENT);
    setDescription("Shows Larp Client activity on your Discord rich presence.");

    this.listeners.add(
        new Listener<TickEvent>("discord_rpc_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            DiscordRPC.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    started = false;
    tickCounter = 0;
    presence = new RichPresence();

    DiscordIPC.setOnError(
        (code, message) -> {
          System.err.println("[Larp Client] Discord IPC error " + code + ": " + message);
        });

    if (!DiscordIPC.start(
        APP_ID,
        () -> {
          started = true;
          tickCounter = 0;
          updatePresence();
        })) {
      System.err.println("[Larp Client] Failed to connect to Discord");
    }
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    DiscordIPC.stop();
    presence = null;
    started = false;
  }

  private void onTick() {
    if (!started) return;
    // Discord rate-limits activity updates: refresh every 5 seconds, not every tick.
    if (tickCounter++ < 100) return;
    tickCounter = 0;
    updatePresence();
  }

  private void updatePresence() {
    if (presence == null || !DiscordIPC.isConnected()) return;

    presence.setDetails("Playing Larp Client");

    ServerData server = minecraft.getCurrentServer();
    if (server != null) {
      String serverName = server.ip;
      if (server.name != null && !server.name.isEmpty()) {
        serverName = server.name;
      }

      ServerStatus.Players players = server.players;
      if (players != null) {
        int online = players.online();
        int max = players.max();
        if (max > 0) {
          presence.setState(serverName + " (" + online + "/" + max + " players)");
        } else {
          presence.setState(serverName);
        }
      } else {
        presence.setState(serverName);
      }
    } else {
      presence.setState("In Singleplayer");
    }

    DiscordIPC.setActivity(presence);
  }
}
