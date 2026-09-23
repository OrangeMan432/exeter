package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.TravelEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public class NoAccel extends ToggleableModule {

  public NoAccel() {
    super("NoAccel", new String[] {"noaccel"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Removes movement acceleration.");

    listeners.add(
        new Listener<TravelEvent>("noaccel_travel") {
          @Override
          public void call(TravelEvent event) {
            NoAccel.this.onTravel(event);
          }
        });
  }

  private void onTravel(TravelEvent event) {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying())
      return;
    if (minecraft.player.isInWater() || minecraft.player.isInLava()) return;
    if (minecraft.player.isPassenger()) return;
    if (!PlayerUtil.isMoving()) return;

    float yaw = minecraft.player.getYRot();
    double forward = minecraft.player.zza;
    double strafe = minecraft.player.xxa;

    if (forward == 0 && strafe == 0) return;

    if (forward != 0 && strafe != 0) {
      forward *= Math.sin(Math.PI / 4);
      strafe *= Math.cos(Math.PI / 4);
    }

    double rad = Math.toRadians(yaw);
    double sin = Math.sin(rad);
    double cos = Math.cos(rad);

    double accelSpeed = minecraft.player.onGround() ? minecraft.player.getSpeed() : getFlyingSpeed();

    double speed = getBaseSpeed() - accelSpeed;
    if (speed < 0) speed = 0;

    double x = (forward * speed * -sin + strafe * speed * cos);
    double z = (forward * speed * cos - strafe * speed * -sin);

    minecraft.player.setDeltaMovement(x, minecraft.player.getDeltaMovement().y, z);
  }

  private double getFlyingSpeed() {
    return 0.054;
  }

  private double getBaseSpeed() {
    double speed = 0.2873;

    if (minecraft.player.hasEffect(MobEffects.SPEED)) {
      speed += 0.2873 * (minecraft.player.getEffect(MobEffects.SPEED).getAmplifier() + 1) * 0.2;
    }

    if (minecraft.player.hasEffect(MobEffects.SLOWNESS)) {
      speed -=
          0.2873 * (minecraft.player.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1) * 0.15;
    }

    return Math.max(speed, 0.2873);
  }
}
