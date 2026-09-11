package me.larp.client.module.impl.toggle.movement;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Shoreline NORMAL-mode staged longjump: boost, hop at 0.42, glide the lead, then friction out.
 * Resets on collision like the original.
 */
public class LongJump extends ToggleableModule {

  private final NumberProperty<Double> boost = new NumberProperty<Double>(4.5, 0.5, 10.0, "Boost");
  private final Property<Boolean> autoDisable = new Property<Boolean>(true, "Auto Disable");

  private int stage;
  private double distance;
  private double speed;

  public LongJump() {
    super("LongJump", new String[] {"longjump", "long-jump"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Staged long jumps for covering ground fast.");
    offerProperties(boost, autoDisable);
    this.listeners.add(
        new Listener<TickEvent>("longjump_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            LongJump.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    stage = 0;
    distance = 0.0;
    speed = 0.0;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    Vec3 delta =
        minecraft
            .player
            .position()
            .subtract(minecraft.player.xOld, minecraft.player.yOld, minecraft.player.zOld);
    distance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

    if (!PlayerUtil.isMoving()) {
      stage = 0;
      return;
    }

    double base = getBaseSpeed();
    if (stage == 0) {
      stage = 1;
      speed = boost.getValue() * base - 0.01;
    } else if (stage == 1) {
      stage = 2;
      minecraft.player.setDeltaMovement(
          minecraft.player.getDeltaMovement().x, 0.42, minecraft.player.getDeltaMovement().z);
      speed *= 2.149;
    } else if (stage == 2) {
      stage = 3;
      double moveSpeed = 0.66 * (distance - base);
      speed = distance - moveSpeed;
    } else {
      if (minecraft.player.horizontalCollision || minecraft.player.verticalCollision) {
        stage = 0;
        if (autoDisable.getValue()) {
          setRunning(false);
          return;
        }
      }
      speed = distance - distance / 159.0;
    }
    speed = Math.max(speed, base);
    setMoveSpeed(speed);
  }

  private void setMoveSpeed(double speed) {
    float yaw = minecraft.player.getYRot();
    double forward = minecraft.player.zza;
    double strafe = minecraft.player.xxa;
    if (forward == 0 && strafe == 0) return;
    double len = Math.sqrt(forward * forward + strafe * strafe);
    forward /= len;
    strafe /= len;
    double rad = Math.toRadians(yaw);
    double sin = Math.sin(rad);
    double cos = Math.cos(rad);
    double newX = forward * -sin * speed + strafe * cos * speed;
    double newZ = forward * cos * speed + strafe * sin * speed;
    minecraft.player.setDeltaMovement(newX, minecraft.player.getDeltaMovement().y, newZ);
  }

  private double getBaseSpeed() {
    double speed = 0.2873;
    if (minecraft.player.hasEffect(MobEffects.SPEED)) {
      speed += 0.2873 * (minecraft.player.getEffect(MobEffects.SPEED).getAmplifier() + 1) * 0.2;
    }
    if (minecraft.player.hasEffect(MobEffects.SLOWNESS)) {
      speed -= 0.2873 * (minecraft.player.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1) * 0.15;
    }
    return Math.max(speed, 0.2873);
  }
}
