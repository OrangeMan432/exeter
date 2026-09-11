package me.larp.client.command.impl.client;

import me.larp.client.command.Argument;
import me.larp.client.command.Command;
import me.larp.client.core.Larp;
import me.larp.client.module.impl.toggle.render.Waypoints;
import net.minecraft.core.BlockPos;

/** .wp add/clear/list for the Waypoints module. */
public final class Wp extends Command {
  public Wp() {
    super(new String[] {"wp", "waypoint"}, new Argument("action"));
  }

  @Override
  public String dispatch() {
    Waypoints waypoints =
        Larp.getInstance().getModuleManager().getModule(Waypoints.class);
    if (waypoints == null) return "Waypoints module missing.";
    String action = this.getArgument("action").getValue();
    if (action == null) return "Usage: .wp <add|clear|list>";
    switch (action.toLowerCase()) {
      case "add" -> {
        if (minecraft.player == null) return "Not in game.";
        BlockPos pos = minecraft.player.blockPosition();
        waypoints.addPoint(pos);
        if (!waypoints.isRunning()) {
          waypoints.setRunning(true);
        }
        return String.format(
            "Waypoint &e#%d&7 at &e%d %d %d&7.",
            waypoints.getPoints().size(), pos.getX(), pos.getY(), pos.getZ());
      }
      case "clear" -> {
        int count = waypoints.getPoints().size();
        waypoints.clearPoints();
        return "Cleared &e" + count + "&7 waypoints.";
      }
      case "list" -> {
        if (waypoints.getPoints().isEmpty()) return "No waypoints.";
        StringBuilder out = new StringBuilder();
        int i = 1;
        for (BlockPos pos : waypoints.getPoints()) {
          out.append(String.format("#%d %d %d %d; ", i++, pos.getX(), pos.getY(), pos.getZ()));
        }
        return out.toString().trim();
      }
      default -> {
        return "Usage: .wp <add|clear|list>";
      }
    }
  }
}
