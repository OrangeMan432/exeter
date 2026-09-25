package me.friendly.exeter.module.impl.toggle.world.fakeplayer.util;

import net.minecraft.world.entity.player.Player;

public class Position {
  private final double x;
  private final double y;
  private final double z;
  private final float yaw;
  private final float pitch;
  private final float headYaw;
  private final double motionX;
  private final double motionY;
  private final double motionZ;

  public Position(Player player) {
    this.x = player.getX();
    this.y = player.getY();
    this.z = player.getZ();
    this.yaw = player.getYRot();
    this.pitch = player.getXRot();
    this.headYaw = player.yHeadRot;
    this.motionX = player.getDeltaMovement().x;
    this.motionY = player.getDeltaMovement().y;
    this.motionZ = player.getDeltaMovement().z;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getZ() {
    return z;
  }

  public float getYaw() {
    return yaw;
  }

  public float getPitch() {
    return pitch;
  }

  public float getHeadYaw() {
    return headYaw;
  }

  public double getMotionX() {
    return motionX;
  }

  public double getMotionY() {
    return motionY;
  }

  public double getMotionZ() {
    return motionZ;
  }
}
