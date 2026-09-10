package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.world.phys.Vec3;

public class ElytraFly extends ToggleableModule {

  private final NumberProperty<Double> speed =
      new NumberProperty<Double>(1.5, 0.1, 5.0, "Speed");
  private final NumberProperty<Double> vertical =
      new NumberProperty<Double>(1.0, 0.0, 3.0, "Vertical");
  private final Property<Boolean> pitchSteer =
      new Property<Boolean>(true, "Pitch Steer");
  private final Property<Boolean> autoTakeoff =
      new Property<Boolean>(false, "Auto Takeoff");

  public ElytraFly() {
    super("ElytraFly", new String[] {"elytrafly", "elytra-fly"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Boosts elytra flight with look steering.");
    offerProperties(speed, vertical, pitchSteer, autoTakeoff);
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
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    if (!minecraft.player.isFallFlying()) {
      if (autoTakeoff.getValue()
          && minecraft.player.isSprinting()
          && !minecraft.player.onGround()
          && minecraft.player.getDeltaMovement().y < -0.1) {
        minecraft.player.startFallFlying();
      }
      return;
    }

    double forward = minecraft.player.zza;
    double strafe = minecraft.player.xxa;
    boolean moving = forward != 0 || strafe != 0;

    Vec3 look = minecraft.player.getLookAngle();
    double hSpeed = speed.getValue();

    double motionX;
    double motionZ;
    double motionY = minecraft.player.getDeltaMovement().y;

    if (pitchSteer.getValue() && moving) {
      // Fly where you look: horizontal from yaw, vertical from pitch.
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
      // Flat boost: keep vanilla glide gravity, add horizontal push.
      double yawRad = Math.toRadians(minecraft.player.getYRot());
      double len = Math.sqrt(forward * forward + strafe * strafe);
      if (len == 0) return;
      double f = forward / len;
      double s = strafe / len;
      double sin = Math.sin(yawRad);
      double cos = Math.cos(yawRad);
      motionX = (f * -sin + s * cos) * hSpeed;
      motionZ = (f * cos + s * sin) * hSpeed;
      motionY = look.y * vertical.getValue();
    }

    minecraft.player.setDeltaMovement(motionX, motionY, motionZ);
  }
}
