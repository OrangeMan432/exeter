package me.friendly.exeter.util;

import java.util.ArrayList;
import java.util.List;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/** Beta player/world helpers. */
public final class PlayerUtil {
  private PlayerUtil() {}

  private static Minecraft mc() {
    return MinecraftAccessor.getMinecraft();
  }

  /** All players in the world without assuming which world list holds them. */
  public static List<PlayerEntity> players() {
    List<PlayerEntity> result = new ArrayList<PlayerEntity>();
    Minecraft mc = mc();
    if (mc == null || mc.world == null) {
      return result;
    }
    World world = mc.world;
    java.util.List[] lists = new java.util.List[] {world.field_198, world.field_199, world.field_200, world.field_201};
    for (int i = 0; i < lists.length; i++) {
      java.util.List list = lists[i];
      if (list == null) continue;
      Object[] copy = list.toArray();
      for (int j = 0; j < copy.length; j++) {
        if (copy[j] instanceof PlayerEntity) {
          result.add((PlayerEntity) copy[j]);
        }
      }
    }
    return result;
  }

  public static double distanceTo(Entity entity) {
    Minecraft mc = mc();
    if (mc == null || mc.player == null || entity == null) {
      return Double.MAX_VALUE;
    }
    double dx = mc.player.x - entity.x;
    double dy = mc.player.y - entity.y;
    double dz = mc.player.z - entity.z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  public static boolean isMoving() {
    Minecraft mc = mc();
    if (mc == null || mc.player == null) {
      return false;
    }
    return mc.player.velocityX != 0.0 || mc.player.velocityZ != 0.0;
  }
}
