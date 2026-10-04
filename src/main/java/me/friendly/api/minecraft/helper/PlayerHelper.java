package me.friendly.api.minecraft.helper;

import net.minecraft.client.Minecraft;

public final class PlayerHelper {
  private static Minecraft minecraft = Minecraft.getInstance();

  public static String getFacingWithProperCapitals() {
    String directionLabel;
    switch (directionLabel = minecraft.getCameraEntity().getDirection().getName()) {
      case "north":
        {
          directionLabel = "North";
          break;
        }
      case "south":
        {
          directionLabel = "South";
          break;
        }
      case "west":
        {
          directionLabel = "West";
          break;
        }
      case "east":
        {
          directionLabel = "East";
        }
    }
    return directionLabel;
  }
}
