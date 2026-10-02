package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.Module;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.NotificationManager;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Elytra flight modes ported from Meteor Client (MIT): Pitch40 oscillates pitch between height
 * bounds, Bounce recasts the glide and holds forward. Only these two modes are ported;
 * Vanilla/Packet need movement-packet control Exeter does not have.
 */
public class ElytraFly extends ToggleableModule {

  public enum FlightMode {
    PITCH40,
    BOUNCE
  }

  public enum YawLock {
    NONE,
    SMART,
    SIMPLE
  }

  private final EnumProperty<FlightMode> mode =
      new EnumProperty<>(FlightMode.BOUNCE, "Mode", "mode");

  private final NumberProperty<Double> pitch40LowerBounds =
      new NumberProperty<>(180.0, -128.0, 360.0, "Lower Bounds", "lowerbounds");
  private final NumberProperty<Double> pitch40UpperBounds =
      new NumberProperty<>(220.0, -128.0, 360.0, "Upper Bounds", "upperbounds");
  private final NumberProperty<Double> pitch40RotationSpeedUp =
      new NumberProperty<>(5.45, 1.0, 20.0, "Rotate Speed Up", "rotatespeedup");
  private final NumberProperty<Double> pitch40RotationSpeedDown =
      new NumberProperty<>(0.90, 0.5, 2.0, "Rotate Speed Down", "rotatespeeddown");
  private final NumberProperty<Integer> pitch40TakeoffDelay =
      new NumberProperty<>(25, 1, 100, "Takeoff Firework Delay", "takeoffdelay");

  private final Property<Boolean> autoJump = new Property<>(true, "Auto Jump", "autojump");
  private final EnumProperty<YawLock> yawLockMode =
      new EnumProperty<>(YawLock.SMART, "Yaw Lock", "yawlock");
  private final EnumProperty<YawLock> pitch40YawLock =
      new EnumProperty<>(YawLock.NONE, "Pitch40 Yaw Lock", "pitch40yawlock");
  private final NumberProperty<Double> yaw = new NumberProperty<>(0.0, 0.0, 360.0, "Yaw", "yaw");
  private final Property<Boolean> lockPitch = new Property<>(true, "Pitch Lock", "pitchlock");
  private final NumberProperty<Double> pitch =
      new NumberProperty<>(85.0, 0.0, 90.0, "Pitch", "pitch");
  private final Property<Boolean> restart = new Property<>(true, "Restart", "restart");
  private final NumberProperty<Integer> restartDelay =
      new NumberProperty<>(7, 0, 20, "Restart Delay", "restartdelay");
  private final Property<Boolean> sprint = new Property<>(true, "Sprint", "sprint");
  private final Property<Boolean> manualTakeoff =
      new Property<>(false, "Manual Takeoff", "manualtakeoff");

  private final Pitch40Mode pitch40 = new Pitch40Mode();
  private final BounceMode bounce = new BounceMode();

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("elytrafly_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != me.friendly.api.event.Stage.PRE) return;
          ElytraFly.this.onTick();
        }
      };

  private final Listener<PacketEvent> packetListener =
      new Listener<PacketEvent>("elytrafly_packet") {
        @Override
        public void call(PacketEvent event) {
          if (event.isSending()) return;
          if (event.getPacket()
              instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket) {
            bounce.onRubberband();
          }
        }
      };

  public ElytraFly() {
    super("ElytraFly", new String[] {"elytrafly", "efly"}, 0x88DDFF, ModuleType.MOVEMENT);
    setDescription("Pitch40 and Bounce elytra flight, ported from Meteor.");
    pitch40LowerBounds.visibleWhen(() -> mode.getValue() == FlightMode.PITCH40);
    pitch40UpperBounds.visibleWhen(() -> mode.getValue() == FlightMode.PITCH40);
    pitch40RotationSpeedUp.visibleWhen(() -> mode.getValue() == FlightMode.PITCH40);
    pitch40RotationSpeedDown.visibleWhen(() -> mode.getValue() == FlightMode.PITCH40);
    pitch40TakeoffDelay.visibleWhen(() -> mode.getValue() == FlightMode.PITCH40);
    pitch40YawLock.visibleWhen(() -> mode.getValue() == FlightMode.PITCH40);
    autoJump.visibleWhen(() -> mode.getValue() == FlightMode.BOUNCE);
    yawLockMode.visibleWhen(() -> mode.getValue() == FlightMode.BOUNCE);
    yaw.visibleWhen(
        () ->
            (mode.getValue() == FlightMode.BOUNCE && yawLockMode.getValue() == YawLock.SIMPLE)
                || (mode.getValue() == FlightMode.PITCH40
                    && pitch40YawLock.getValue() == YawLock.SIMPLE));
    lockPitch.visibleWhen(() -> mode.getValue() == FlightMode.BOUNCE);
    pitch.visibleWhen(() -> mode.getValue() == FlightMode.BOUNCE && lockPitch.getValue());
    restart.visibleWhen(() -> mode.getValue() == FlightMode.BOUNCE);
    restartDelay.visibleWhen(() -> mode.getValue() == FlightMode.BOUNCE && restart.getValue());
    sprint.visibleWhen(() -> mode.getValue() == FlightMode.BOUNCE);
    manualTakeoff.visibleWhen(() -> mode.getValue() == FlightMode.BOUNCE);
    offerProperties(
        mode,
        pitch40LowerBounds,
        pitch40UpperBounds,
        pitch40RotationSpeedUp,
        pitch40RotationSpeedDown,
        pitch40TakeoffDelay,
        pitch40YawLock,
        autoJump,
        yawLockMode,
        yaw,
        lockPitch,
        pitch,
        restart,
        restartDelay,
        sprint,
        manualTakeoff);
    listeners.add(tickListener);
    listeners.add(packetListener);
  }

  public static ElytraFly get() {
    if (Exeter.getInstance() == null) return null;
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("elytrafly");
    return module instanceof ElytraFly ? (ElytraFly) module : null;
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    if (mode.getValue() == FlightMode.PITCH40) {
      pitch40.onActivate();
    } else {
      bounce.onActivate();
    }
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    pitch40.onDeactivate();
    bounce.onDeactivate();
  }

  @Override
  public String getTag() {
    if (mode.getValue() == FlightMode.PITCH40) {
      return pitch40.isTakingOff() ? "Takeoff" : "Pitch40";
    }
    return "Bounce";
  }

  private void onTick() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.level == null) return;
    if (mode.getValue() == FlightMode.PITCH40) {
      pitch40.onTick();
    } else {
      bounce.onTick();
      bounce.onPreTick();
    }
  }

  private static double randPitch(double pitch, double bound) {
    return pitch + (bound * (Math.random() - 0.5));
  }

  private float resolveYaw(YawLock lock) {
    Minecraft mc = Minecraft.getInstance();
    if (lock == YawLock.SIMPLE) {
      return yaw.getValue().floatValue();
    }
    if (lock == YawLock.SMART) {
      return Math.round((mc.player.getYRot() + 1f) / 45f) * 45f;
    }
    return mc.player.getYRot();
  }

  private final class Pitch40Mode {
    private boolean pitchingDown = true;
    private boolean takingOff;
    private float pitchValue = 37.72F;
    private int takeoffTicks;
    private int flyTicks;
    private boolean outNotified;
    private double lastY;
    private int boostCooldown;

    void onActivate() {
      Minecraft mc = Minecraft.getInstance();
      pitchingDown = true;
      pitchValue = 37.72F;
      takeoffTicks = 0;
      flyTicks = 0;
      outNotified = false;
      takingOff = !mc.player.isFallFlying();
      lastY = mc.player.getY();
      boostCooldown = 0;
    }

    void onDeactivate() {
      takingOff = false;
      flyTicks = 0;
    }

    boolean isTakingOff() {
      return takingOff;
    }

    void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.player.isFallFlying()) {
        takingOff = true;
        takeoffTick();
        return;
      }
      if (takingOff) {
        if (mc.player.getY() >= pitch40LowerBounds.getValue()) {
          takingOff = false;
          pitchingDown = false;
          pitchValue = -54.77F;
          lastY = mc.player.getY();
        } else {
          takeoffTick();
          return;
        }
      }
      if (pitchingDown && mc.player.getY() <= pitch40LowerBounds.getValue()) {
        pitchingDown = false;
        lastY = mc.player.getY();
      } else if (!pitchingDown && mc.player.getY() >= pitch40UpperBounds.getValue()) {
        pitchingDown = true;
      }
      if (!pitchingDown) {
        if (boostCooldown > 0) {
          boostCooldown--;
        } else if (mc.player.getY() < lastY - 0.05
            && mc.player.getY() < pitch40UpperBounds.getValue()) {
          // Backsliding before the crest: boost once, then cool down.
          if (fireRocket()) {
            boostCooldown = 30;
          } else if (!outNotified) {
            outNotified = true;
            NotificationManager.push("ElytraFly: out of fireworks", "warning");
          }
        }
        lastY = mc.player.getY();
        pitchValue -= randPitch(pitch40RotationSpeedUp.getValue(), 1.0);
        if (pitchValue < -54.77F) {
          pitchValue = -54.77F;
          pitchingDown = true;
        }
      } else if (pitchValue < 37.72F) {
        pitchValue += randPitch(pitch40RotationSpeedDown.getValue(), 0.50);
      }
      PlayerUtil.setRotation(resolveYaw(pitch40YawLock.getValue()), pitchValue);
    }

    private void takeoffTick() {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.player.isFallFlying()) {
        flyTicks = 0;
        if (!airspaceClear()) {
          NotificationManager.push("ElytraFly: airspace blocked above", "error");
          ElytraFly.this.setRunning(false);
          return;
        }
        if (!hasGlider()) {
          NotificationManager.push("ElytraFly: no elytra equipped", "error");
          ElytraFly.this.setRunning(false);
          return;
        }
        // Relaunch with a sprint-jump: deploying with zero airspeed stalls out
        // instantly (redeploy/land flap). Sprint is released once flight is set.
        if (mc.player.onGround()) {
          mc.player.setSprinting(true);
          mc.player.jumpFromGround();
          return;
        }
        if (mc.player.fallDistance > 0.0f && mc.player.tryToStartFallFlying()) {
          mc.getConnection()
              .send(
                  new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(
                      mc.player,
                      net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action
                          .START_FALL_FLYING));
        }
        return;
      }
      flyTicks++;
      if (flyTicks > 30) {
        mc.player.setSprinting(false);
      }
      PlayerUtil.setRotation(resolveYaw(pitch40YawLock.getValue()), -90.0);
      takeoffTicks++;
      if (takeoffTicks >= pitch40TakeoffDelay.getValue()) {
        takeoffTicks = 0;
        if (!fireRocket()) {
          if (!outNotified) {
            outNotified = true;
            NotificationManager.push("ElytraFly: out of fireworks", "warning");
          }
        } else {
          outNotified = false;
        }
      }
      if (mc.player.getY() >= pitch40LowerBounds.getValue()) {
        takingOff = false;
        pitchingDown = false;
        pitchValue = -54.77F;
      }
    }

    private boolean hasGlider() {
      Minecraft mc = Minecraft.getInstance();
      return mc.player
          .getItemBySlot(EquipmentSlot.CHEST)
          .has(net.minecraft.core.component.DataComponents.GLIDER);
    }

    private boolean airspaceClear() {
      Minecraft mc = Minecraft.getInstance();
      int x = (int) Math.floor(mc.player.getX());
      int z = (int) Math.floor(mc.player.getZ());
      int baseY = (int) Math.floor(mc.player.getY());
      for (int i = 1; i <= 45; i++) {
        if (!mc.level.getBlockState(new net.minecraft.core.BlockPos(x, baseY + i, z)).isAir()) {
          return false;
        }
      }
      return true;
    }

    private boolean fireRocket() {
      Minecraft mc = Minecraft.getInstance();
      int slot =
          PlayerUtil.findInHotbar(
              stack ->
                  !stack.isEmpty() && stack.is(net.minecraft.world.item.Items.FIREWORK_ROCKET));
      if (slot == -1) {
        return false;
      }
      boolean needSwitch = slot != mc.player.getInventory().getSelectedSlot();
      if (needSwitch) {
        PlayerUtil.swapTo(slot);
      }
      mc.getConnection()
          .send(
              new net.minecraft.network.protocol.game.ServerboundUseItemPacket(
                  net.minecraft.world.InteractionHand.MAIN_HAND,
                  0,
                  mc.player.getYRot(),
                  mc.player.getXRot()));
      PlayerUtil.swingHand();
      if (needSwitch) {
        PlayerUtil.swapBack();
      }
      return true;
    }
  }

  private final class BounceMode {
    private boolean rubberbanded;
    private int tickDelay;
    private double prevFov;

    void onActivate() {
      Minecraft mc = Minecraft.getInstance();
      prevFov = mc.options.fovEffectScale().get();
      tickDelay = restartDelay.getValue();
      rubberbanded = false;
    }

    void onDeactivate() {
      unpress();
      rubberbanded = false;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && prevFov != 0 && !sprint.getValue()) {
        mc.options.fovEffectScale().set(prevFov);
      }
    }

    void onRubberband() {
      rubberbanded = true;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
        mc.player.stopFallFlying();
      }
    }

    void onPreTick() {
      Minecraft mc = Minecraft.getInstance();
      if (checkConditions(mc.player) && sprint.getValue()) {
        mc.player.setSprinting(true);
      }
    }

    void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options.keyJump.isDown() && !mc.player.isFallFlying() && !manualTakeoff.getValue()) {
        mc.getConnection()
            .send(
                new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(
                    mc.player,
                    net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action
                        .START_FALL_FLYING));
      }
      if (!checkConditions(mc.player)) {
        return;
      }
      if (!rubberbanded) {
        if (prevFov != 0 && !sprint.getValue()) {
          mc.options.fovEffectScale().set(0.0);
        }
        if (autoJump.getValue()) {
          mc.options.keyJump.setDown(true);
        }
        mc.options.keyUp.setDown(true);
        PlayerUtil.setRotation(getYawDirection(), mc.player.getXRot());
        if (lockPitch.getValue()) {
          PlayerUtil.setRotation(mc.player.getYRot(), pitch.getValue().floatValue());
        }
      }
      if (!sprint.getValue()) {
        if (mc.player.isFallFlying()) {
          mc.player.setSprinting(mc.player.onGround());
        } else {
          mc.player.setSprinting(true);
        }
      }
      if (rubberbanded && restart.getValue()) {
        if (tickDelay > 0) {
          tickDelay--;
        } else {
          mc.getConnection()
              .send(
                  new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(
                      mc.player,
                      net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action
                          .START_FALL_FLYING));
          rubberbanded = false;
          tickDelay = restartDelay.getValue();
        }
      }
    }

    private void unpress() {
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.options == null) return;
      mc.options.keyUp.setDown(false);
      if (autoJump.getValue()) {
        mc.options.keyJump.setDown(false);
      }
    }

    private float getYawDirection() {
      return resolveYaw(yawLockMode.getValue());
    }

    private boolean checkConditions(Player player) {
      if (player == null) return false;
      BlockState blockState = player.getInBlockState();
      boolean climbing =
          blockState.is(BlockTags.CLIMBABLE) && !blockState.is(BlockTags.CAN_GLIDE_THROUGH);
      return !player.getAbilities().flying
          && !player.isPassenger()
          && !climbing
          && !player.isInWater()
          && !player.hasEffect(MobEffects.LEVITATION);
    }

    @SuppressWarnings("unused")
    private boolean startGliding(Player player) {
      for (EquipmentSlot slot : EquipmentSlot.values()) {
        if (LivingEntity.canGlideUsing(player.getItemBySlot(slot), slot)) {
          player.startFallFlying();
          return true;
        }
      }
      return false;
    }
  }

  @SuppressWarnings("unused")
  public static boolean recastElytra(Player player) {
    ElytraFly fly = get();
    if (fly == null) return false;
    return fly.bounce.checkConditions(player) && fly.bounce.startGliding(player);
  }
}
