package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Port of Lemon's TargetStrafe: orbits the nearest target at Preferred Distance using the NCP stage
 * engine, speeding in a straight line past Max Distance. Jumps to keep the stages fed and flips
 * orbit direction when stuck on a wall.
 */
public class TargetStrafe extends ToggleableModule {

  private final NumberProperty<Integer> targetRange =
      new NumberProperty<Integer>(20, 0, 256, "Target Range");
  private final Property<Boolean> jump = new Property<Boolean>(true, "Jump");
  private final Property<Boolean> antiStuck = new Property<Boolean>(true, "Anti Stuck");
  private final NumberProperty<Double> preferredDistance =
      new NumberProperty<Double>(1.0, 0.0, 10.0, "Preferred Distance");
  private final NumberProperty<Double> maxDistance =
      new NumberProperty<Double>(10.0, 1.0, 32.0, "Max Distance");
  private final NumberProperty<Double> turnAmount =
      new NumberProperty<Double>(5.0, 1.0, 90.0, "Turn Amount");

  private int direction = 1;
  private int level = 1;
  private double moveSpeed;
  private double lastDist;
  private double boostSpeed;
  private long boostTimer;
  private boolean wasOnGround;
  private String lastTargetKey;

  public TargetStrafe() {
    super("TargetStrafe", new String[] {"targetstrafe", "strafe"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Orbits the nearest player.");
    offerProperties(targetRange, jump, antiStuck, preferredDistance, maxDistance, turnAmount);
    targetRange.setDescription("Nearest targetable player within this many blocks.");
    jump.setDescription("Hop to keep the speed stages fed while orbiting.");
    antiStuck.setDescription("Flip orbit direction when stuck against a wall.");
    preferredDistance.setDescription("Orbit ring radius around the target.");
    maxDistance.setDescription("Past this range, run straight at the target instead.");
    turnAmount.setDescription("Degrees steered inward or outward per tick to hold the ring.");
    listeners.add(
        new Listener<PacketEvent>("targetstrafe_packet") {
          @Override
          public void call(PacketEvent event) {
            TargetStrafe.this.onPacket(event);
          }
        });
    listeners.add(
        new Listener<TickEvent>("targetstrafe_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            TargetStrafe.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    direction = 1;
    level = 1;
    moveSpeed = getBaseSpeed();
    lastDist = 0;
    boostSpeed = 0;
    wasOnGround = false;
    lastTargetKey = null;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    lastTargetKey = null;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  @Override
  public String getTag() {
    return lastTargetKey;
  }

  private void onPacket(PacketEvent event) {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying())
      return;
    if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
      // Lagback: travelled distance is meaningless now, restart the stages clean.
      lastDist = 0;
      moveSpeed = getBaseSpeed();
    } else if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet) {
      if (packet.id() == minecraft.player.getId()) {
        Vec3 vel = packet.movement();
        boostSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
      }
    }
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying())
      return;
    Player target = findTarget();
    if (target == null || isLiquid() || inWeb()) {
      lastTargetKey = null;
      return;
    }
    String targetKey = target.getName().getString();
    if (!targetKey.equals(lastTargetKey)) {
      lastTargetKey = targetKey;
      DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "target=" + targetKey);
    }

    // Landing edge, not every grounded tick: re-arming stage 2 continuously
    // multiplies the boost without bound when Jump can't lift us off.
    boolean onGround = minecraft.player.onGround();
    if (onGround && !wasOnGround) {
      level = 2;
    }
    wasOnGround = onGround;

    boolean noInput = minecraft.player.zza == 0 && minecraft.player.xxa == 0;
    if (level != 1 || noInput) {
      if (level == 2) {
        level = 3;
        if (jump.getValue() && minecraft.player.onGround()) {
          minecraft.player.jumpFromGround();
        }
        moveSpeed *= 1.433;
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

    if (PlayerUtil.isMoving() && boostSpeed != 0 && System.currentTimeMillis() - boostTimer > 1) {
      moveSpeed = boostSpeed;
      boostTimer = System.currentTimeMillis();
      boostSpeed = 0;
    }
    moveSpeed = Math.max(moveSpeed, getBaseSpeed());

    if (minecraft.player.horizontalCollision && antiStuck.getValue()) {
      direction = -direction;
    }
    strafeTowards(target, moveSpeed);

    Vec3 delta =
        minecraft
            .player
            .position()
            .subtract(minecraft.player.xOld, minecraft.player.yOld, minecraft.player.zOld);
    lastDist = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
  }

  /**
   * Orbits at Preferred Distance: perpendicular to the line to the target, angled inward/outward by
   * Turn Amount, straight at them past Max Distance. Jumps to feed the stage engine. Lemon's 1.12
   * low-hop micro-offsets are skipped (NCP-specific).
   */
  private void strafeTowards(Player target, double speed) {
    double dx = target.getX() - minecraft.player.getX();
    double dz = target.getZ() - minecraft.player.getZ();
    float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
    float orbitYaw = yaw + 90.0F * direction;
    double distance = Math.sqrt(dx * dx + dz * dz);
    if (distance < maxDistance.getValue()) {
      if (distance > preferredDistance.getValue()) {
        orbitYaw -= turnAmount.getValue() * direction;
      } else if (distance < preferredDistance.getValue()) {
        orbitYaw += turnAmount.getValue() * direction;
      }
    } else {
      orbitYaw = yaw;
    }
    if (jump.getValue() && minecraft.player.onGround()) {
      minecraft.player.jumpFromGround();
    }
    double angle = Math.toRadians(orbitYaw + 90.0);
    minecraft.player.setDeltaMovement(
        speed * Math.cos(angle), minecraft.player.getDeltaMovement().y, speed * Math.sin(angle));
  }

  private Player findTarget() {
    Player best = null;
    double bestDist = targetRange.getValue();
    for (Player player : minecraft.level.players()) {
      if (player == minecraft.player || !player.isAlive()) continue;
      if (!Exeter.getInstance().getFriendManager().isTargetable(player.getName().getString()))
        continue;
      double dist = minecraft.player.distanceTo(player);
      if (dist < bestDist) {
        bestDist = dist;
        best = player;
      }
    }
    return best;
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

  private boolean inWeb() {
    for (BlockPos pos :
        BlockPos.betweenClosed(
            BlockPos.containing(
                minecraft.player.getBoundingBox().minX,
                minecraft.player.getBoundingBox().minY,
                minecraft.player.getBoundingBox().minZ),
            BlockPos.containing(
                minecraft.player.getBoundingBox().maxX,
                minecraft.player.getBoundingBox().maxY,
                minecraft.player.getBoundingBox().maxZ))) {
      if (minecraft.level.getBlockState(pos).is(Blocks.COBWEB)) return true;
    }
    return false;
  }
}
