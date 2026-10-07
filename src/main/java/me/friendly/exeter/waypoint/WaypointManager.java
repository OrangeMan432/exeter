package me.friendly.exeter.waypoint;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import me.friendly.api.registry.ListRegistry;
import me.friendly.exeter.config.Config;
import net.minecraft.client.Minecraft;

/** Owns the waypoint list and persists it to waypoints.json. */
public final class WaypointManager extends ListRegistry<Waypoint> {

  private final Config config;

  public WaypointManager() {
    this.registry = new ArrayList();
    this.config =
        new Config("waypoints.json") {
          @Override
          public void load(Object... source) {
            loadWaypoints();
          }

          @Override
          public void save(Object... destination) {
            saveWaypoints();
          }
        };
  }

  public Waypoint getWaypoint(String name) {
    for (Waypoint waypoint : getRegistry()) {
      if (waypoint.getName().equalsIgnoreCase(name)) {
        return waypoint;
      }
    }
    return null;
  }

  public boolean add(Waypoint waypoint) {
    if (getWaypoint(waypoint.getName()) != null) {
      return false;
    }
    getRegistry().add(waypoint);
    saveWaypoints();
    return true;
  }

  public boolean remove(String name) {
    Waypoint waypoint = getWaypoint(name);
    if (waypoint == null) {
      return false;
    }
    getRegistry().remove(waypoint);
    saveWaypoints();
    return true;
  }

  /**
   * Pins the player's current position under the first free "Waypoint N" name. Returns the created
   * waypoint, or null when there is no player to pin.
   */
  public Waypoint addHere() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.level == null) {
      return null;
    }
    int n = getRegistry().size() + 1;
    while (getWaypoint("Waypoint " + n) != null) {
      n++;
    }
    Waypoint waypoint =
        new Waypoint(
            "Waypoint " + n,
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ(),
            mc.level.dimension().identifier().toString(),
            true);
    getRegistry().add(waypoint);
    saveWaypoints();
    return waypoint;
  }

  /**
   * Parses {@code name [x y z]} from the window and commands. A bare name pins the player's current
   * position. Returns null on success, an error message otherwise.
   */
  public String addWaypoint(String[] parts) {
    String name = parts[0];
    double x;
    double y;
    double z;
    Minecraft mc = Minecraft.getInstance();
    if (parts.length == 1) {
      if (mc.player == null || mc.level == null) {
        return "No position: join a world or give x y z.";
      }
      x = mc.player.getX();
      y = mc.player.getY();
      z = mc.player.getZ();
    } else if (parts.length == 4) {
      try {
        x = Double.parseDouble(parts[1]);
        y = Double.parseDouble(parts[2]);
        z = Double.parseDouble(parts[3]);
      } catch (NumberFormatException e) {
        return "Coords must be numbers.";
      }
    } else {
      return "Usage: name [x y z].";
    }
    String dimension =
        mc.level != null ? mc.level.dimension().identifier().toString() : "minecraft:overworld";
    if (!add(new Waypoint(name, x, y, z, dimension, true))) {
      return "Waypoint '" + name + "' already exists.";
    }
    return null;
  }

  public void saveWaypoints() {
    JsonArray array = new JsonArray();
    for (Waypoint waypoint : getRegistry()) {
      JsonObject entry = new JsonObject();
      entry.addProperty("name", waypoint.getName());
      entry.addProperty("x", waypoint.getX());
      entry.addProperty("y", waypoint.getY());
      entry.addProperty("z", waypoint.getZ());
      entry.addProperty("dimension", waypoint.getDimension());
      entry.addProperty("enabled", waypoint.isEnabled());
      entry.addProperty("color", waypoint.getColor());
      array.add(entry);
    }
    try {
      if (config.getFile().getParentFile() != null) {
        config.getFile().getParentFile().mkdirs();
      }
      try (FileWriter writer = new FileWriter(config.getFile())) {
        writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(array));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void loadWaypoints() {
    if (!config.getFile().exists()) {
      return;
    }
    try (FileReader reader = new FileReader(config.getFile())) {
      JsonElement root = new JsonParser().parse(reader);
      if (!(root instanceof JsonArray array)) {
        return;
      }
      for (JsonElement node : array) {
        if (!(node instanceof JsonObject entry)) {
          continue;
        }
        try {
          getRegistry()
              .add(
                  new Waypoint(
                      entry.get("name").getAsString(),
                      entry.get("x").getAsDouble(),
                      entry.get("y").getAsDouble(),
                      entry.get("z").getAsDouble(),
                      entry.has("dimension")
                          ? entry.get("dimension").getAsString()
                          : "minecraft:overworld",
                      !entry.has("enabled") || entry.get("enabled").getAsBoolean(),
                      entry.has("color") ? entry.get("color").getAsInt() : 0x55FFFF));
        } catch (RuntimeException e) {
          e.printStackTrace();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
