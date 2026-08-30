package me.friendly.exeter.command.impl.server;

import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public final class Connect extends Command {
  public Connect() {
    super(new String[] {"connect", "c"}, new Argument("ip"));
  }

  @Override
  public String dispatch() {
    ServerData serverData =
        new ServerData("", this.getArgument("ip").getValue(), ServerData.Type.OTHER);
    this.minecraft.disconnect(null, false);
    this.minecraft.setLevel(null);
    ConnectScreen.startConnecting(
        null, this.minecraft, ServerAddress.parseString(serverData.ip), serverData, false, null);
    return "Connecting...";
  }
}
