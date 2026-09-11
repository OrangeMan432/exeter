package me.larp.client.module.impl.toggle.client;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.status.ServerStatus;

public class DiscordRPC extends ToggleableModule {

  private static final long APP_ID = 1468971656407027888L;

  private RichPresence presence;
  private boolean started;
  private int tickCounter;

  private final NumberProperty<Integer> updateSeconds =
      new NumberProperty<Integer>(5, 1, 60, "Update Seconds");

  public DiscordRPC() {
    super("DiscordRPC", new String[] {"discordrpc", "rpc"}, 0x5865F2, ModuleType.CLIENT);
    setDescription("Shows Larp Client activity on your Discord rich presence.");
    offerProperties(updateSeconds);

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
    // Discord rate-limits activity updates: refresh on interval, not every tick.
    if (tickCounter++ < updateSeconds.getValue() * 20) return;
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
