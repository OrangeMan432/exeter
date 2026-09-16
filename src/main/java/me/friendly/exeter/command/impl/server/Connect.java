package me.friendly.exeter.command.impl.server;

import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public final class Connect extends Command {
  public Connect() {
    super(new String[] {"connect", "c"}, new Argument("ip"));
    setDescription("Connect to a server");
  }

  @Override
  public String dispatch() {
    String ip = this.getArgument("ip").getValue();
    ServerData serverData = new ServerData("", ip, ServerData.Type.OTHER);
    ServerAddress address = ServerAddress.parseString(ip);
    // must run on client thread; PacketEvent may be on netty thread
    this.minecraft.execute(
        () -> {
          try {
            ConnectScreen.startConnecting(null, this.minecraft, address, serverData, false, null);
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
    return "Connecting to &e" + ip + "&7...";
  }
}
