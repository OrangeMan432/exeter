package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Freely rotates the camera with the mouse while the player keeps facing its own direction.
 * Concept ported from Meteor Client's FreeLook. Vanilla mouse deltas are measured, reverted on
 * the player, and applied to detached camera angles instead. The beta camera setup
 * (class_555.method_1851) temporarily sees the camera angles via the mixin.
 */
public final class FreeLook extends ToggleableModule {

  public final Property<Boolean> togglePerspective =
      new Property<Boolean>(true, "Toggle Perspective", "perspective");
  public final NumberProperty<Double> sensitivity =
      new NumberProperty<Double>(1.0, 0.0, 3.0, "Sensitivity", "sensitivity");

  private float cameraYaw;
  private float cameraPitch;
  private float preYaw;
  private float prePitch;
  private boolean prePerspective;

  public FreeLook() {
    super("FreeLook", new String[] {"freelook", "free-look"}, 0x55FFFF, ModuleType.RENDER);
    setDescription("Rotate the camera with the mouse without turning the player.");
    offerProperties(togglePerspective, sensitivity);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null) return;
    if (mc.player != null) {
      cameraYaw = mc.player.yaw;
      cameraPitch = mc.player.pitch;
    }
    // mc.options is null during early config load; perspective applies on next enable.
    if (mc.options == null) return;
    prePerspective = mc.options.thirdPerson;
    if (togglePerspective.getValue().booleanValue() && !prePerspective) {
      mc.options.thirdPerson = true;
    }
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.options == null) return;
    if (togglePerspective.getValue().booleanValue()
        && mc.options.thirdPerson != prePerspective) {
      mc.options.thirdPerson = prePerspective;
    }
  }

  public static FreeLook get() {
    if (Exeter.getInstance() == null) return null;
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("freelook");
    return module instanceof FreeLook ? (FreeLook) module : null;
  }

  public static boolean isFreeLooking() {
    FreeLook look = get();
    if (look == null || !look.isRunning()) return false;
    Minecraft mc = MinecraftAccessor.getMinecraft();
    return mc != null && mc.player != null;
  }

  public float getCameraYaw() {
    return cameraYaw;
  }

  public float getCameraPitch() {
    return cameraPitch;
  }

  /** Records the player rotation before the vanilla mouse turn runs. */
  public void recordPlayerRotation() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null) return;
    preYaw = mc.player.yaw;
    prePitch = mc.player.pitch;
  }

  /**
   * Reverts the vanilla player turn and applies the measured delta to the detached camera
   * angles instead.
   */
  public void redirectTurn() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null) return;
    PlayerEntity player = mc.player;
    if (player == null) return;
    float deltaYaw = player.yaw - preYaw;
    float deltaPitch = player.pitch - prePitch;
    player.yaw = preYaw;
    player.pitch = prePitch;
    float scale = sensitivity.getValue().floatValue();
    cameraYaw += deltaYaw * scale;
    float next = cameraPitch + deltaPitch * scale;
    if (next > 90.0F) next = 90.0F;
    if (next < -90.0F) next = -90.0F;
    cameraPitch = next;
  }

  /** Swaps the player angles for the camera angles during camera setup. */
  public void swapToCamera() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null) return;
    preYaw = mc.player.yaw;
    prePitch = mc.player.pitch;
    mc.player.yaw = cameraYaw;
    mc.player.pitch = cameraPitch;
    // Camera setup interpolates prev -> current; sync both so it does not jitter.
    mc.player.prevYaw = cameraYaw;
    mc.player.prevPitch = cameraPitch;
  }

  /** Restores the player angles after camera setup. */
  public void restorePlayer() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null) return;
    mc.player.yaw = preYaw;
    mc.player.pitch = prePitch;
  }
}
