package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Combatant-pattern ElytraFly for 26.2: BOOST pitch steering, STATIC direct
 * control, VANILLA drag physics, FIREWORK auto-boost. Elytra + durability
 * gated like the original.
 */
public class ElytraFly extends ToggleableModule {

  public enum Mode {
    BOOST,
    STATIC,
    VANILLA,
    FIREWORK
  }

  private final EnumProperty<Mode> mode = new EnumProperty<Mode>(Mode.BOOST, "Mode");
  private final NumberProperty<Double> speed =
      new NumberProperty<Double>(1.5, 0.1, 5.0, "Speed");
  private final NumberProperty<Double> vertical =
      new NumberProperty<Double>(1.0, 0.0, 3.0, "Vertical");
  private final NumberProperty<Double> drag =
      new NumberProperty<Double>(0.99, 0.9, 1.0, "Drag");
  private final NumberProperty<Double> lift =
      new NumberProperty<Double>(0.06, 0.0, 0.3, "Lift");
  private final Property<Boolean> pitchSteer =
      new Property<Boolean>(true, "Pitch Steer");
  private final Property<Boolean> autoTakeoff =
      new Property<Boolean>(false, "Auto Takeoff");

  public ElytraFly() {
    super("ElytraFly", new String[] {"elytrafly", "elytra-fly"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Combatant-pattern elytra engine.");
    offerProperties(speed, vertical, drag, lift, pitchSteer, autoTakeoff, mode);
    this.listeners.add(
        new Listener<TickEvent>("elytrafly_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            ElytraFly.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (minecraft.player.isPassenger()) return;
    if (minecraft.player.getAbilities().instabuild) return;
    if (minecraft.player.hasEffect(MobEffects.LEVITATION)) return;

    if (!minecraft.player.isFallFlying()) {
      if (autoTakeoff.getValue()
          && minecraft.player.isSprinting()
          && !minecraft.player.onGround()
          && minecraft.player.getDeltaMovement().y < -0.1
          && hasElytra()) {
        minecraft.player.startFallFlying();
      }
      return;
    }

    switch (mode.getValue()) {
      case BOOST -> tickBoost();
      case STATIC -> tickStatic();
      case VANILLA -> tickVanilla();
      case FIREWORK -> tickFirework();
    }
  }

  /** Pitch steering with jump-key boost. */
  private void tickBoost() {
    double forward = minecraft.player.zza;
    double strafe = minecraft.player.xxa;
    if (forward == 0 && strafe == 0) return;

    double hSpeed = speed.getValue();
    if (minecraft.options.keyJump.isDown()) {
      hSpeed *= 1.5;
    }

    double motionX;
    double motionZ;
    double motionY;
    if (pitchSteer.getValue()) {
      double yawRad = Math.toRadians(minecraft.player.getYRot());
      double pitchRad = Math.toRadians(minecraft.player.getXRot());
      double hFactor = Math.cos(pitchRad);
      motionX = -Math.sin(yawRad) * hFactor * hSpeed;
      motionZ = Math.cos(yawRad) * hFactor * hSpeed;
      motionY = -Math.sin(pitchRad) * hSpeed * vertical.getValue();
      if (forward < 0) {
        motionX = -motionX;
        motionZ = -motionZ;
        motionY = -motionY;
      }
    } else {
      double yawRad = Math.toRadians(minecraft.player.getYRot());
      double len = Math.sqrt(forward * forward + strafe * strafe);
      double f = forward / len;
      double s = strafe / len;
      motionX = (f * -Math.sin(yawRad) + s * Math.cos(yawRad)) * hSpeed;
      motionZ = (f * Math.cos(yawRad) + s * Math.sin(yawRad)) * hSpeed;
      motionY = minecraft.player.getLookAngle().y * vertical.getValue();
    }
    minecraft.player.setDeltaMovement(motionX, motionY, motionZ);
  }

  /** Direct strafe control, jump/shift for vertical. */
  private void tickStatic() {
    double forward = minecraft.player.zza;
    double strafe = minecraft.player.xxa;
    double yawRad = Math.toRadians(minecraft.player.getYRot());
    double motionX = 0;
    double motionZ = 0;
    if (forward != 0 || strafe != 0) {
      double len = Math.sqrt(forward * forward + strafe * strafe);
      double f = forward / len;
      double s = strafe / len;
      double h = speed.getValue();
      motionX = (f * -Math.sin(yawRad) + s * Math.cos(yawRad)) * h;
      motionZ = (f * Math.cos(yawRad) + s * Math.sin(yawRad)) * h;
    }
    double motionY;
    if (minecraft.options.keyJump.isDown()) {
      motionY = vertical.getValue();
    } else if (minecraft.options.keyShift.isDown()) {
      motionY = -vertical.getValue();
    } else {
      motionY = -0.01;
    }
    minecraft.player.setDeltaMovement(motionX, motionY, motionZ);
  }

  /** Vanilla glide physics, amplified. */
  private void tickVanilla() {
    Vec3 vel = minecraft.player.getDeltaMovement();
    Vec3 look = minecraft.player.getViewVector(1.0f);
    float pitch = minecraft.player.getXRot();
    vel = vel.scale(drag.getValue());
    vel = vel.add(look.scale(speed.getValue() * 0.02));
    double liftY = -Math.sin(Math.toRadians(pitch)) * lift.getValue();
    minecraft.player.setDeltaMovement(vel.x, vel.y + liftY, vel.z);
  }

  /** Burns a firework whenever gliding slow. */
  private void tickFirework() {
    tickBoost();
    Vec3 vel = minecraft.player.getDeltaMovement();
    double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
    if (hSpeed > speed.getValue()) return;
    int rocketSlot = PlayerUtil.findInHotbar(
        s -> !s.isEmpty() && s.getItem() == Items.FIREWORK_ROCKET);
    if (rocketSlot == -1) return;
    boolean needSwitch =
        rocketSlot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(rocketSlot);
    }
    minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
    if (needSwitch) {
      PlayerUtil.swapBack();
    }
  }

  private boolean hasElytra() {
    return minecraft.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
  }
}
