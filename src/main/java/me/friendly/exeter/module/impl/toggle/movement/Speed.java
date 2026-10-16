package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

public class Speed extends ToggleableModule {

  /**
   * Fraction of the boost kept each airborne tick. Tapered so a boost ends on its own if the player
   * somehow never touches ground, and tuned so a typical knockback carries most of the arc.
   */
  private static final double BOOST_DECAY = 0.96;

  /** Boost below this is treated as spent. */
  private static final double BOOST_MIN = 0.05;

  /** Below this the player has no meaningful horizontal motion to scale. */
  private static final double MIN_HORIZONTAL = 1.0E-4;

  private final Property<Boolean> damageBoost = new Property<Boolean>(true, "Damage Boost");
  private final Property<Boolean> jump = new Property<Boolean>(true, "Jump");
  private final Property<Boolean> strict = new Property<Boolean>(false, "Strict");
  private final Property<Boolean> waterSpeed = new Property<Boolean>(true, "Water Speed");
  private final Property<Boolean> randomBoost = new Property<Boolean>(false, "Random Boost");

  private int level = 1;
  private double moveSpeed;
  private double lastDist;
  private double boostSpeed;
  private long lastRandomBoost;
  private boolean lagDetected;
  private long detectionTime;

  public Speed() {
    super("Speed", new String[] {"speed"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Increases movement speed with various methods.");

    offerProperties(damageBoost, jump, strict, waterSpeed, randomBoost);
    damageBoost.setDescription("Gain speed after taking damage.");
    jump.setDescription("Jump to keep moving.");
    strict.setDescription("Use slower but safer speeds.");
    waterSpeed.setDescription("Keep moving fast while in liquids.");
    randomBoost.setDescription("Add random speed bursts while running.");

    listeners.add(
        new Listener<PacketEvent>("speed_packet") {
          @Override
          public void call(PacketEvent event) {
            Speed.this.onPacketReceive(event);
          }
        });

    listeners.add(
        new Listener<TickEvent>("speed_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Speed.this.onTick(event);
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    if (minecraft.player == null) {
      // Enabled by config restore during startup: Speed is a short-burst module,
      // so it always starts disabled and must be toggled on in-game.
      setRunning(false);
      return;
    }
    boostSpeed = 0;
    lastRandomBoost = System.currentTimeMillis();
    moveSpeed = getBaseSpeed();
    level = 1;
  }

  private void onPacketReceive(PacketEvent event) {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying())
      return;

    if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet) {
      if (packet.id() == minecraft.player.getId()) {
        detectionTime = System.currentTimeMillis();
        lagDetected = true;
        Vec3 vel = packet.movement();
        boostSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        DebugLogger.get()
            .log(
                getLabel(),
                DebugLogger.Level.INFO,
                "damage boost armed at " + round(boostSpeed) + " blocks per tick");
      }
    }
  }

  private void onTick(TickEvent event) {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying())
      return;

    if (System.currentTimeMillis() - detectionTime > 3182) {
      lagDetected = false;
    }

    // The damage boost is ticked and applied outside the movement path below, so it lands with
    // or without a key held, and it gets the last word over the stage speed instead of being
    // overwritten by it.
    tickDamageBoost();
    applyStageSpeed();
    applyDamageBoost();
  }

  private void applyStageSpeed() {
    if (jump.getValue() && PlayerUtil.isMoving() && minecraft.player.onGround()) {
      minecraft.player.jumpFromGround();
    }

    Vec3 delta =
        minecraft
            .player
            .position()
            .subtract(minecraft.player.xOld, minecraft.player.yOld, minecraft.player.zOld);
    lastDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

    boolean moving = PlayerUtil.isMoving();
    if (!moving) return;

    if (!waterSpeed.getValue() && isLiquid()) return;

    if (minecraft.player.onGround()) {
      level = 2;
    }

    if (level != 1) {
      if (level == 2) {
        level = 3;
        if (!strict.getValue() && !minecraft.player.isShiftKeyDown()) {
          moveSpeed *= 1.64847275;
        } else {
          moveSpeed *= 1.433;
        }
      } else if (level == 3) {
        level = 4;
        moveSpeed = lastDist - 0.6553 * (lastDist - getBaseSpeed() + 0.04);
      } else {
        if (minecraft.player.onGround()) {
          level = 1;
        }
        moveSpeed = lastDist - lastDist / 201.0;
      }
    } else {
      level = 2;
      moveSpeed = 1.418 * getBaseSpeed();
    }

    if (randomBoost.getValue()
        && System.currentTimeMillis() - lastRandomBoost > 3500
        && !lagDetected
        && moving
        && minecraft.player.onGround()) {
      moveSpeed += moveSpeed / 6.0;
      lastRandomBoost = System.currentTimeMillis();
    }

    moveSpeed = Math.max(moveSpeed, getBaseSpeed());

    setMoveSpeed(moveSpeed);
  }

  /**
   * Keeps a pending damage boost alive for the whole airtime instead of spending it in one tick.
   *
   * <p>The knockback magnitude is recorded when the damage packet arrives and re-applied every tick
   * while the player is still airborne and still pressing a key, tapering as it goes. The boost
   * amplifies movement rather than replacing it, so it ends as soon as the input stops.
   */
  private void tickDamageBoost() {
    if (boostSpeed <= 0.0) return;

    if (minecraft.player.onGround()) {
      clearDamageBoost("landed");
      return;
    }

    // Without input nothing else writes horizontal velocity, so re-applying the boost each tick
    // would cancel the game's air drag and accelerate the player off the map. Hand control back to
    // the game instead.
    if (!PlayerUtil.isMoving()) {
      clearDamageBoost("movement input released");
      return;
    }

    boostSpeed *= BOOST_DECAY;
    if (boostSpeed < BOOST_MIN) {
      clearDamageBoost("tapered to " + round(boostSpeed) + " blocks per tick");
    }
  }

  private void clearDamageBoost(String reason) {
    if (boostSpeed <= 0.0) return;
    DebugLogger.get()
        .log(getLabel(), DebugLogger.Level.INFO, "damage boost ended (" + reason + ")");
    boostSpeed = 0.0;
  }

  /**
   * Re-applies the boost as the last step of the tick.
   *
   * <p>Velocity is scaled rather than rebuilt from the movement input, so holding a strafe key
   * cannot cancel the boost, and the knockback direction is preserved. It only runs while a
   * movement key is held, so releasing the keys hands the horizontal velocity back to the game and
   * the player stops instead of drifting. Knockback with no horizontal component has nothing to
   * scale, so that case launches along the facing instead.
   */
  private void applyDamageBoost() {
    if (!damageBoost.getValue() || boostSpeed <= 0.0 || !PlayerUtil.isMoving()) return;

    Vec3 velocity = minecraft.player.getDeltaMovement();
    double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    double target = Math.max(horizontal, boostSpeed);

    if (horizontal < MIN_HORIZONTAL) {
      double rad = Math.toRadians(minecraft.player.getYRot());
      minecraft.player.setDeltaMovement(
          -Math.sin(rad) * target, velocity.y, Math.cos(rad) * target);
      return;
    }

    double scale = target / horizontal;
    minecraft.player.setDeltaMovement(velocity.x * scale, velocity.y, velocity.z * scale);
  }

  private static double round(double value) {
    return Math.round(value * 1000.0) / 1000.0;
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

    if (minecraft.player == null) return speed;

    if (minecraft.player.hasEffect(MobEffects.SPEED)) {
      speed += 0.2873 * (minecraft.player.getEffect(MobEffects.SPEED).getAmplifier() + 1) * 0.2;
    }

    if (minecraft.player.hasEffect(MobEffects.SLOWNESS)) {
      speed -= 0.2873 * (minecraft.player.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1) * 0.15;
    }

    return Math.max(speed, 0.2873);
  }

  private boolean isLiquid() {
    return minecraft.player.isInWater() || minecraft.player.isInLava();
  }
}
