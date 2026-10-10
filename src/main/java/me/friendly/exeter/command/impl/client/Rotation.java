package me.friendly.exeter.command.impl.client;

import me.friendly.exeter.command.Argument;
import me.friendly.exeter.command.Command;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.util.Mth;

/**
 * Port of Meteor's rotation command: set or nudge the camera rotation. Runs through the normal
 * rotation path so freemove-style spoofs keep working.
 */
public final class Rotation extends Command {
  public Rotation() {
    super(new String[] {"rotation", "rot"}, new Argument("mode"), new Argument("pitch"));
    setDescription("Set or adjust your rotation");
    addSubCommand("set", "<pitch> [yaw]", "Set pitch, optionally yaw.");
    addSubCommand("add", "<pitch> [yaw]", "Nudge pitch, optionally yaw.");
    addSubCommand("<direction>", "", "Face north, south, east, west, up, down or forwards.");
  }

  @Override
  public String dispatch(String[] input) {
    if (minecraft.player == null) {
      return "No player.";
    }
    if (input.length == 2) {
      return faceDirection(input[1].toUpperCase());
    }
    if (input.length < 3 || input.length > 4) {
      return String.format("%s %s", input[0], this.getSyntax());
    }
    float pitch;
    try {
      pitch = Float.parseFloat(input[2]);
    } catch (NumberFormatException e) {
      return "Invalid pitch: " + input[2];
    }
    Float yaw = null;
    if (input.length == 4) {
      try {
        yaw = Float.parseFloat(input[3]);
      } catch (NumberFormatException e) {
        return "Invalid yaw: " + input[3];
      }
    }
    String mode = input[1].toLowerCase();
    if (mode.equals("set")) {
      float clamped = Math.min(Math.max(pitch, -90.0F), 90.0F);
      PlayerUtil.setRotation(yaw == null ? minecraft.player.getYRot() : yaw, clamped);
      return yaw == null
          ? String.format("Pitch set to %.1f.", pitch)
          : String.format("Rotation set to %.1f %.1f.", pitch, yaw);
    }
    if (mode.equals("add")) {
      float newPitch = minecraft.player.getXRot() + pitch;
      newPitch = newPitch >= 0 ? Math.min(newPitch, 90.0F) : Math.max(newPitch, -90.0F);
      float newYaw =
          yaw == null
              ? minecraft.player.getYRot()
              : Mth.wrapDegrees(minecraft.player.getYRot() + yaw);
      PlayerUtil.setRotation(newYaw, newPitch);
      return String.format("Rotation now %.1f %.1f.", newPitch, newYaw);
    }
    return String.format("%s %s", input[0], this.getSyntax());
  }

  @Override
  public String dispatch() {
    return "Specify set, add or a direction.";
  }

  private String faceDirection(String name) {
    float yaw;
    float pitch = 0.0F;
    switch (name) {
      case "NORTH":
        yaw = 180.0F;
        break;
      case "SOUTH":
        yaw = 0.0F;
        break;
      case "WEST":
        yaw = 90.0F;
        break;
      case "EAST":
        yaw = -90.0F;
        break;
      case "UP":
        yaw = minecraft.player.getYRot();
        pitch = -90.0F;
        break;
      case "DOWN":
        yaw = minecraft.player.getYRot();
        pitch = 90.0F;
        break;
      case "FORWARDS":
      case "FORWARD":
        yaw = minecraft.player.getYRot();
        pitch = 0.0F;
        break;
      default:
        return String.format("%s %s", "rotation", this.getSyntax());
    }
    PlayerUtil.setRotation(yaw, pitch);
    return "Facing " + name.toLowerCase() + ".";
  }
}
