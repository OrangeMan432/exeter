package me.larp.api.minecraft.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;

public final class PlayerHelper {
  private static Minecraft minecraft = Minecraft.getInstance();

  public static Block getBlockBelowPlayer(double height) {
    return WorldHelper.getBlock(
        PlayerHelper.minecraft.player.getX(),
        PlayerHelper.minecraft.player.getY() - height,
        PlayerHelper.minecraft.player.getZ());
  }

  public static Block getBlockAbovePlayer(double height) {
    return WorldHelper.getBlock(
        PlayerHelper.minecraft.player.getX(),
        PlayerHelper.minecraft.player.getY() + height,
        PlayerHelper.minecraft.player.getZ());
  }

  public static boolean isInLiquid() {
    if (PlayerHelper.minecraft.player == null) {
      return false;
    }
    boolean inLiquid = false;
    int y = (int) PlayerHelper.minecraft.player.getBoundingBox().minY;
    for (int x = Mth.floor(PlayerHelper.minecraft.player.getBoundingBox().minX);
        x < Mth.floor(PlayerHelper.minecraft.player.getBoundingBox().maxX) + 1;
        ++x) {
      for (int z = Mth.floor(PlayerHelper.minecraft.player.getBoundingBox().minZ);
          z < Mth.floor(PlayerHelper.minecraft.player.getBoundingBox().maxZ) + 1;
          ++z) {
        Block block = PlayerHelper.minecraft.level.getBlockState(new BlockPos(x, y, z)).getBlock();
        if (block == null || block instanceof AirBlock) continue;
        if (!(block instanceof LiquidBlock)) {
          return false;
        }
        if (block instanceof LiquidBlock) {
          return true;
        }
        inLiquid = true;
      }
    }
    return inLiquid;
  }

  public static boolean isInLiquid(double offset) {
    return PlayerHelper.getBlockBelowPlayer(-offset) instanceof LiquidBlock;
  }

  public static boolean isOnLiquid() {
    AABB boundingBox = PlayerHelper.minecraft.player.getBoundingBox();
    boundingBox = boundingBox.inflate(0.0, 0.0, 0.0).move(0.0, -0.02, 0.0);
    boolean onLiquid = false;
    int y = (int) boundingBox.minY;
    for (int x = Mth.floor(boundingBox.minX); x < Mth.floor(boundingBox.maxX + 1.0); ++x) {
      for (int z = Mth.floor(boundingBox.minZ); z < Mth.floor(boundingBox.maxZ + 1.0); ++z) {
        Block block = WorldHelper.getBlock(x, y, z);
        if (block == Blocks.AIR) continue;
        if (!(block instanceof LiquidBlock)) {
          return false;
        }
        onLiquid = true;
      }
    }
    return onLiquid;
  }

  public static boolean isAiming(float yaw, float pitch, int fov) {
    float pitchDiff;
    yaw = PlayerHelper.wrapAngleTo180(yaw);
    pitch = PlayerHelper.wrapAngleTo180(pitch);
    float curYaw = PlayerHelper.wrapAngleTo180(PlayerHelper.minecraft.player.getYRot());
    float curPitch = PlayerHelper.wrapAngleTo180(PlayerHelper.minecraft.player.getXRot());
    float yawDiff = Math.abs(yaw - curYaw);
    return yawDiff + (pitchDiff = Math.abs(pitch - curPitch)) <= (float) fov;
  }

  public static float getFOV(float[] rotations) {
    float yaw = rotations[0];
    float pitch = rotations[1];
    yaw = PlayerHelper.wrapAngleTo180(yaw);
    pitch = PlayerHelper.wrapAngleTo180(pitch);
    float curYaw = PlayerHelper.wrapAngleTo180(PlayerHelper.minecraft.player.getYRot());
    float curPitch = PlayerHelper.wrapAngleTo180(PlayerHelper.minecraft.player.getXRot());
    float yawDiff = Math.abs(yaw - curYaw);
    float pitchDiff = Math.abs(pitch - curPitch);
    return yawDiff + pitchDiff;
  }

  public static float wrapAngleTo180(float angle) {
    if ((angle %= 360.0f) >= 180.0f) {
      angle -= 360.0f;
    }
    if (angle < -180.0f) {
      angle += 360.0f;
    }
    return angle;
  }

  public static boolean isMoving() {
    return (double) PlayerHelper.minecraft.player.xxa != 0.0
        || (double) PlayerHelper.minecraft.player.zza != 0.0;
  }

  public static boolean isPressingMoveKeybinds() {
    return PlayerHelper.minecraft.options.keyUp.isDown()
        || PlayerHelper.minecraft.options.keyDown.isDown()
        || PlayerHelper.minecraft.options.keyLeft.isDown()
        || PlayerHelper.minecraft.options.keyRight.isDown();
  }

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
