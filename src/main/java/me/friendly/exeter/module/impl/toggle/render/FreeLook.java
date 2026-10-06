package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.MathUtil;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.client.CameraType;

/**
 * Freely rotates the camera with the mouse while the player keeps facing its own direction.
 *
 * <p>Concept ported from Meteor Client's FreeLook (derived from
 * https://github.com/MeteorDevelopment/meteor-client, MIT-licensed original adapted here). Vanilla
 * mouse deltas are measured, reverted on the player, and applied to detached camera angles instead,
 * so no vanilla look math is reimplemented.
 */
public final class FreeLook extends ToggleableModule {

  public final Property<Boolean> togglePerspective =
      new Property<Boolean>(true, "Toggle Perspective");
  public final NumberProperty<Double> sensitivity =
      new NumberProperty<Double>(1.0, 0.0, 3.0, "Sensitivity");

  private float cameraYaw;
  private float cameraPitch;
  private float preYaw;
  private float prePitch;
  private CameraType prePerspective;

  public FreeLook() {
    super("FreeLook", new String[] {"freelook", "free-look"}, 0x55FFFF, ModuleType.RENDER);
    setDescription("Rotate the camera with the mouse without turning the player.");
    offerProperties(togglePerspective, sensitivity);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    if (minecraft.player != null) {
      cameraYaw = minecraft.player.getYRot();
      cameraPitch = minecraft.player.getXRot();
    }
    prePerspective = minecraft.options.getCameraType();
    if (togglePerspective.getValue() && prePerspective.isFirstPerson()) {
      minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "Enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    if (togglePerspective.getValue()
        && prePerspective != null
        && minecraft.options.getCameraType() != prePerspective) {
      minecraft.options.setCameraType(prePerspective);
    }
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "Disabled");
  }

  public boolean isFreeLooking() {
    return isRunning() && minecraft.player != null;
  }

  public float getCameraYaw() {
    return cameraYaw;
  }

  public float getCameraPitch() {
    return cameraPitch;
  }

  /** Records the player rotation before vanilla mouse look runs. */
  public void recordPlayerRotation() {
    preYaw = minecraft.player.getYRot();
    prePitch = minecraft.player.getXRot();
  }

  /**
   * Reverts vanilla's player turn and applies the measured delta to the detached camera angles
   * instead.
   */
  public void redirectTurn() {
    float deltaYaw = minecraft.player.getYRot() - preYaw;
    float deltaPitch = minecraft.player.getXRot() - prePitch;
    PlayerUtil.restoreRotation(preYaw, prePitch);
    float scale = sensitivity.getValue().floatValue();
    cameraYaw += deltaYaw * scale;
    cameraPitch = MathUtil.clamp(cameraPitch + deltaPitch * scale, -90.0f, 90.0f);
  }
}
