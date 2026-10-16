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
  private final float walkSpeed;
  private final float walkPosition;
  private final boolean swinging;
  private final boolean offhandSwing;

  public Position(Player player) {
    this(player, 1.0f);
  }

  /**
   * @param partialTick interpolation factor for the limb animation, so a playback frame lands on
   *     the same swing phase as the recorded frame
   */
  public Position(Player player, float partialTick) {
    this.x = player.getX();
    this.y = player.getY();
    this.z = player.getZ();
    this.yaw = player.getYRot();
    this.pitch = player.getXRot();
    this.headYaw = player.yHeadRot;
    this.motionX = player.getDeltaMovement().x;
    this.motionY = player.getDeltaMovement().y;
    this.motionZ = player.getDeltaMovement().z;
    this.walkSpeed = player.walkAnimation.speed(partialTick);
    this.walkPosition = player.walkAnimation.position(partialTick);
    this.swinging = player.isSwinging();
    var swing = player.getCurrentSwing();
    this.offhandSwing =
        swing != null && swing.hand() == net.minecraft.world.InteractionHand.OFF_HAND;
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

  public float getWalkSpeed() {
    return walkSpeed;
  }

  public float getWalkPosition() {
    return walkPosition;
  }

  public boolean isSwinging() {
    return swinging;
  }

  public boolean isOffhandSwing() {
    return offhandSwing;
  }
}
