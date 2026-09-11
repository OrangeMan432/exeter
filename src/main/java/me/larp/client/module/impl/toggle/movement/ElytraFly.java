package me.larp.client.module.impl.toggle.movement;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Combatant-pattern ElytraFly for 26.2: BOOST pitch steering, STATIC direct control, VANILLA drag
 * physics, FIREWORK auto-boost. Elytra + durability gated like the original.
 */
public class ElytraFly extends ToggleableModule {

  public enum Mode {
    BOOST,
    STATIC,
    VANILLA,
    FIREWORK,
    GLIDE,
    BOUNCE
  }

  private enum GlideState {
    CRUISE,
    DIVE,
    CLIMB
  }

  private final EnumProperty<Mode> mode = new EnumProperty<Mode>(Mode.BOOST, "Mode");
  private final NumberProperty<Double> speed = new NumberProperty<Double>(1.5, 0.1, 5.0, "Speed");
  private final NumberProperty<Double> vertical =
      new NumberProperty<Double>(1.0, 0.0, 3.0, "Vertical");
  private final NumberProperty<Double> drag = new NumberProperty<Double>(0.99, 0.9, 1.0, "Drag");
  private final NumberProperty<Double> lift = new NumberProperty<Double>(0.06, 0.0, 0.3, "Lift");
  private final Property<Boolean> pitchSteer = new Property<Boolean>(true, "Pitch Steer");
  private final Property<Boolean> autoTakeoff =
      new Property<Boolean>(true, "Auto Takeoff");
  private final Property<Boolean> autoEquip =
      new Property<Boolean>(true, "Auto Equip");
  private final NumberProperty<Integer> deployDelay =
      new NumberProperty<Integer>(10, 0, 40, "Deploy Delay");
  private final Property<Boolean> durabilityGuard = new Property<Boolean>(true, "Durability Guard");
  private final NumberProperty<Integer> minDurability =
      new NumberProperty<Integer>(5, 1, 50, "Min Durability");
  private final NumberProperty<Integer> cruiseAltitude =
      new NumberProperty<Integer>(180, 0, 320, "Cruise Altitude");
  private final NumberProperty<Double> stallSpeed =
      new NumberProperty<Double>(14.0, 5.0, 40.0, "Stall Speed");
  private final NumberProperty<Double> momentumSpeed =
      new NumberProperty<Double>(27.0, 10.0, 60.0, "Momentum Speed");
  private final NumberProperty<Double> diveAngle =
      new NumberProperty<Double>(38.0, 0.0, 90.0, "Dive Angle");
  private final NumberProperty<Double> climbAngle =
      new NumberProperty<Double>(40.0, 0.0, 90.0, "Climb Angle");
  private final NumberProperty<Double> pitchStep =
      new NumberProperty<Double>(10.0, 1.0, 40.0, "Pitch Step");
  private final NumberProperty<Integer> bounceDelay =
      new NumberProperty<Integer>(1, 0, 20, "Bounce Delay");
  private final NumberProperty<Integer> rocketCooldown =
      new NumberProperty<Integer>(3500, 500, 10000, "Rocket Cooldown");

  private int tickCounter;
  private int deployTick = -100;
  private final double[] speedSamples = new double[10];
  private int speedSampleIndex;
  private double speedAvg;
  private double lastX;
  private double lastZ;
  private double cruisePhase;
  private GlideState glideState = GlideState.CRUISE;
  private boolean climbingToTarget;
  private long lastRocketTime;
  private int sinceJump;
  private int sinceFalling;

  public ElytraFly() {
    super("ElytraFly", new String[] {"elytrafly", "elytra-fly"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Combatant-pattern elytra engine.");
    offerProperties(
        speed,
        vertical,
        drag,
        lift,
        pitchSteer,
        autoTakeoff,
        autoEquip,
        deployDelay,
        durabilityGuard,
        minDurability,
        cruiseAltitude,
        stallSpeed,
        momentumSpeed,
        diveAngle,
        climbAngle,
        pitchStep,
        bounceDelay,
        rocketCooldown,
        mode);
    this.listeners.add(
        new Listener<TickEvent>("elytrafly_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            ElytraFly.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
    deployTick = -100;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (minecraft.player.isPassenger()) return;
    if (minecraft.player.getAbilities().instabuild) return;
    if (minecraft.player.hasEffect(MobEffects.LEVITATION)) return;
    tickCounter++;

    // BlackOut-pattern durability guard: never break the elytra mid-flight.
    if (durabilityGuard.getValue()) {
      ItemStack chest = minecraft.player.getItemBySlot(EquipmentSlot.CHEST);
      if (chest.is(Items.ELYTRA)
          && (chest.getMaxDamage() - chest.getDamageValue()) < minDurability.getValue()) {
        setRunning(false);
        return;
      }
    }
    trackSpeed();

    // Auto-equip an elytra from inventory so takeoff never fails on armor.
    if (autoEquip.getValue() && !hasElytra()) {
      for (int i = 9; i < 45; i++) {
        ItemStack stack = minecraft.player.containerMenu.getSlot(i).getItem();
        if (!stack.isEmpty() && stack.is(Items.ELYTRA)) {
          minecraft.gameMode.handleContainerInput(
              minecraft.player.containerMenu.containerId,
              i,
              0,
              net.minecraft.world.inventory.ContainerInput.QUICK_MOVE,
              minecraft.player);
          break;
        }
      }
    }

    if (mode.getValue() == Mode.BOUNCE) {
      tickBounce();
      return;
    }

    if (!minecraft.player.isFallFlying()) {
      if (autoTakeoff.getValue()
          && !minecraft.player.onGround()
          && !minecraft.player.isInWater()
          && !minecraft.player.isInLava()
          && minecraft.player.getDeltaMovement().y < -0.1
          && hasElytra()) {
        deploy();
      }
      return;
    }

    // Deploy grace: the server needs a few ticks to accept fall-flying state.
    // Boosting earlier is what causes the rubberband.
    if (tickCounter - deployTick < deployDelay.getValue()) {
      return;
    }

    switch (mode.getValue()) {
      case BOOST -> tickBoost();
      case STATIC -> tickStatic();
      case VANILLA -> tickVanilla();
      case FIREWORK -> tickFirework();
      case GLIDE -> tickGlide();
      case BOUNCE -> tickBounce();
    }
  }

  /** Starts fall flight client + server side and stamps the deploy tick. */
  private void deploy() {
    minecraft.player.startFallFlying();
    minecraft
        .getConnection()
        .send(
            new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(
                minecraft.player,
                net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action
                    .START_FALL_FLYING));
    deployTick = tickCounter;
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

  /** BlackOut-pattern glide autopilot: climb to cruise, dive on stall. */
  private void tickGlide() {
    if (!minecraft.player.isFallFlying()) return;
    double y = minecraft.player.getY();
    long now = System.currentTimeMillis();

    if (!climbingToTarget && y < cruiseAltitude.getValue() - 10) {
      climbingToTarget = true;
    }
    GlideState state;
    if (climbingToTarget) {
      state = GlideState.CLIMB;
      if (now - lastRocketTime >= rocketCooldown.getValue() && useRocket()) {
        lastRocketTime = now;
      }
      if (y >= cruiseAltitude.getValue() + 2) {
        climbingToTarget = false;
      }
    } else {
      if (speedAvg <= stallSpeed.getValue()) {
        glideState = GlideState.DIVE;
      } else if (speedAvg >= momentumSpeed.getValue()) {
        glideState = GlideState.CRUISE;
      }
      state = glideState;
    }

    float targetPitch;
    if (state == GlideState.DIVE) {
      targetPitch = diveAngle.getValue().floatValue();
    } else if (state == GlideState.CLIMB) {
      float base = -climbAngle.getValue().floatValue();
      targetPitch = speedAvg < 15 ? base / 2f : base;
    } else {
      cruisePhase += 0.019;
      double tri = 2.0 * Math.abs(2.0 * (cruisePhase - Math.floor(cruisePhase + 0.5))) - 1.0;
      targetPitch = (float) -(4.0 + 8.0 * (0.5 * (tri + 1.0)));
    }
    minecraft.player.setXRot(
        net.minecraft.util.Mth.approach(
            minecraft.player.getXRot(), targetPitch, pitchStep.getValue().floatValue()));
  }

  /** BlackOut-pattern bounce: jump on touchdown, redeploy in the air. */
  private void tickBounce() {
    minecraft.player.setSprinting(true);
    if (sinceFalling <= 1 && minecraft.player.onGround()) {
      minecraft.player.jumpFromGround();
      sinceJump = 0;
    } else if (sinceJump > bounceDelay.getValue() && !minecraft.player.isFallFlying()) {
      deploy();
    }
    sinceJump++;
    sinceFalling = minecraft.player.isFallFlying() ? 0 : sinceFalling + 1;
    if (minecraft.player.isFallFlying()) {
      tickBoost();
    }
  }

  private void trackSpeed() {
    double dx = minecraft.player.getX() - lastX;
    double dz = minecraft.player.getZ() - lastZ;
    speedSamples[speedSampleIndex] = Math.sqrt(dx * dx + dz * dz) * 20.0;
    speedSampleIndex = (speedSampleIndex + 1) % speedSamples.length;
    double sum = 0;
    for (double s : speedSamples) sum += s;
    speedAvg = sum / speedSamples.length;
    lastX = minecraft.player.getX();
    lastZ = minecraft.player.getZ();
  }

  private boolean useRocket() {
    int rocketSlot =
        PlayerUtil.findInHotbar(s -> !s.isEmpty() && s.getItem() == Items.FIREWORK_ROCKET);
    if (rocketSlot == -1) return false;
    boolean needSwitch = rocketSlot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(rocketSlot);
    }
    minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
    if (needSwitch) {
      PlayerUtil.swapBack();
    }
    return true;
  }

  /** Burns a firework whenever gliding slow. */
  private void tickFirework() {
    tickBoost();
    Vec3 vel = minecraft.player.getDeltaMovement();
    double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
    if (hSpeed > speed.getValue()) return;
    int rocketSlot =
        PlayerUtil.findInHotbar(s -> !s.isEmpty() && s.getItem() == Items.FIREWORK_ROCKET);
    if (rocketSlot == -1) return;
    boolean needSwitch = rocketSlot != minecraft.player.getInventory().getSelectedSlot();
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
