package me.friendly.exeter.command.impl.client;

import java.util.Arrays;
import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.waypoint.Waypoint;
import me.friendly.exeter.waypoint.WaypointManager;

public final class Waypoints {

  private static WaypointManager manager() {
    return Exeter.getInstance().getWaypointManager();
  }

  public static final class Add extends Command {
    public Add() {
      super(new String[] {"wpadd", "wpa"});
      setDescription("Add a waypoint (bare name pins your position)");
    }

    @Override
    public String dispatch(String[] input) {
      if (input.length < 2) {
        return "Usage: wpadd <name> [x y z].";
      }
      String[] parts = Arrays.copyOfRange(input, 1, input.length);
      String error = manager().addWaypoint(parts);
      return error == null ? "Added waypoint (" + parts[0] + ")." : error;
    }

    @Override
    public String dispatch() {
      return "Usage: wpadd <name> [x y z].";
    }
  }

  public static final class Here extends Command {
    public Here() {
      super(new String[] {"wphere", "wph"});
      setDescription("Pin your current position (optional name)");
    }

    @Override
    public String dispatch(String[] input) {
      if (input.length > 1) {
        String[] parts = Arrays.copyOfRange(input, 1, input.length);
        String error = manager().addWaypoint(parts);
        return error == null ? "Added waypoint (" + parts[0] + ")." : error;
      }
      Waypoint pinned = manager().addHere();
      return pinned == null
          ? "No position: join a world first."
          : "Added waypoint (" + pinned.getName() + ").";
    }

    @Override
    public String dispatch() {
      return "Usage: wphere [name].";
    }
  }

  public static final class Remove extends Command {
    public Remove() {
      super(new String[] {"wpremove", "wprem"}, new Argument("name"));
      setDescription("Remove a waypoint");
    }

    @Override
    public String dispatch() {
      String name = this.getArgument("name").getValue();
      if (!manager().remove(name)) {
        return "No waypoint named '" + name + "'.";
      }
      return "Removed waypoint (" + name + ").";
    }
  }

  public static final class List extends Command {
    public List() {
      super(new String[] {"wplist", "wpl"});
      setDescription("List waypoints");
    }

    @Override
    public String dispatch() {
      if (manager().getRegistry().isEmpty()) {
        return "No waypoints.";
      }
      StringBuilder out = new StringBuilder();
      for (Waypoint waypoint : manager().getRegistry()) {
        if (out.length() > 0) {
          out.append(", ");
        }
        out.append(waypoint.isEnabled() ? "&a" : "&8")
            .append(waypoint.getName())
            .append("&7 (")
            .append(waypoint.coordsShort())
            .append(")");
      }
      return out.toString();
    }
  }
}
