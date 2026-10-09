package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import me.friendly.exeter.util.ExplosionUtil;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Crystal aura in the spirit of 3arthh4ck's AutoCrystal, cut down to the working core:
 * score placements by raw explosion damage, place the best, break the best. No
 * prediction, motion extrapolation or multiplace.
 */
public class AutoCrystal extends ToggleableModule {

  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(10.0, 0.0, 16.0, "Target Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 0.0, 6.0, "Place Range");
  private final NumberProperty<Double> wallRange =
      new NumberProperty<Double>(3.0, 0.0, 6.0, "Wall Range");
  private final NumberProperty<Double> breakRange =
      new NumberProperty<Double>(5.0, 0.0, 6.0, "Break Range");
  private final NumberProperty<Double> minDamage =
      new NumberProperty<Double>(4.0, 0.0, 36.0, "Min Damage");
  private final NumberProperty<Double> maxSelfDamage =
      new NumberProperty<Double>(8.0, 0.0, 36.0, "Max Self Damage");
  private final NumberProperty<Integer> placeDelay =
      new NumberProperty<Integer>(4, 0, 40, "Place Delay");
  private final NumberProperty<Integer> breakDelay =
      new NumberProperty<Integer>(4, 0, 40, "Break Delay");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> switchBack = new Property<Boolean>(true, "Switch Back");

  private int tickCounter;
  private int lastPlaceTick = -100;
  private int lastBreakTick = -100;
  private String lastTargetKey;
  private boolean noCrystalLogged;
  private boolean noWallLogged;
  private BlockPos pendingCrystal;

  public AutoCrystal() {
    super("AutoCrystal", new String[] {"autocrystal", "crystal", "ca"}, 0xFF44FF, ModuleType.COMBAT);
    setDescription("Places and breaks end crystals on targets.");
    offerProperties(
        targetRange,
        placeRange,
        wallRange,
        breakRange,
        minDamage,
        maxSelfDamage,
        placeDelay,
        breakDelay,
        rotate,
        swingHand,
        autoSwitch,
        switchBack);
    this.listeners.add(
        new Listener<TickEvent>("autocrystal_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });
    this.listeners.add(
        new Listener<PacketEvent>("autocrystal_packet") {
          @Override
          public void call(PacketEvent event) {
            PlayerUtil.spoofMovement(event);
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    lastTargetKey = null;
    noCrystalLogged = false;
    noWallLogged = false;
    pendingCrystal = null;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    lastTargetKey = null;
    noCrystalLogged = false;
    noWallLogged = false;
    pendingCrystal = null;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  private void onTick() {
    tickCounter++;
    if (minecraft.player == null || minecraft.level == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    Player target = findTarget();
    if (target == null) {
      lastTargetKey = null;
      pendingCrystal = null;
      return;
    }
    String targetKey = target.getName().getString();
    if (!targetKey.equals(lastTargetKey)) {
      lastTargetKey = targetKey;
      noCrystalLogged = false;
      noWallLogged = false;
      pendingCrystal = null;
      DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "target=" + targetKey);
    }

    tickBreak(target);
    tickPlace(target);
  }

  private Player findTarget() {
    List<Player> players = new ArrayList<>();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player) continue;
      if (!entity.isAlive()) continue;
      if (!(entity instanceof Player player)) continue;
      if (!Exeter.getInstance().getFriendManager().isTargetable(player.getName().getString())) {
        continue;
      }
      if (minecraft.player.distanceTo(player) > targetRange.getValue()) continue;
      players.add(player);
    }
    return players.stream()
        .min(Comparator.comparingDouble(p -> minecraft.player.distanceTo(p)))
        .orElse(null);
  }

  /** Raw end-crystal damage (power 6) at a detonation point. Pre-armor estimate. */
  private float damageAt(Vec3 center, Player victim) {
    return ExplosionUtil.explosionDamage(minecraft.level, center, 6.0F, victim);
  }

  private void tickBreak(Player target) {
    if (tickCounter - lastBreakTick < breakDelay.getValue()) return;
    if (breakPending()) return;
    EndCrystal best = null;
    float bestDamage = 0.0f;
    Vec3 eye = minecraft.player.getEyePosition();
    double rangeSq = breakRange.getValue() * breakRange.getValue();
    for (Entity entity : minecraft.level.getEntities(null, target.getBoundingBox().inflate(12.0))) {
      if (!(entity instanceof EndCrystal crystal)) continue;
      if (eye.distanceToSqr(crystal.position()) > rangeSq) continue;
      float targetDamage = damageAt(crystal.position(), target);
      if (targetDamage < minDamage.getValue()) continue;
      float selfDamage = damageAt(crystal.position(), minecraft.player);
      if (selfDamage > maxSelfDamage.getValue()) continue;
      if (targetDamage > bestDamage) {
        bestDamage = targetDamage;
        best = crystal;
      }
    }
    if (best == null) return;
    float selfDamage = damageAt(best.position(), minecraft.player);
    minecraft.gameMode.attack(minecraft.player, best);
    if (swingHand.getValue()) PlayerUtil.swingHand();
    DebugLogger.get()
        .logFile(
            getLabel(),
            "break crystal at "
                + best.blockPosition().toShortString()
                + " targetDmg="
                + bestDamage
                + " selfDmg="
                + selfDamage);
    lastBreakTick = tickCounter;
  }

  /**
   * Breaks our pending crystal first, ignoring target damage: the target may have left
   * its blast area, but it still blocks future placements. Returns true when a break
   * was attempted (one break per tick budget).
   */
  private boolean breakPending() {
    if (pendingCrystal == null) return false;
    Vec3 center = Vec3.atCenterOf(pendingCrystal);
    EndCrystal crystal = null;
    for (Entity entity :
        minecraft.level.getEntities(null, targetBox(pendingCrystal.below()))) {
      if (entity instanceof EndCrystal c
          && c.isAlive()
          && c.position().distanceToSqr(center) < 4.0) {
        crystal = c;
        break;
      }
    }
    if (crystal == null) {
      pendingCrystal = null;
      return false;
    }
    float selfDamage = damageAt(crystal.position(), minecraft.player);
    if (selfDamage > maxSelfDamage.getValue()) {
      // Won't touch it even to unblock: drop it and move on instead of stalling.
      pendingCrystal = null;
      return false;
    }
    minecraft.gameMode.attack(minecraft.player, crystal);
    if (swingHand.getValue()) PlayerUtil.swingHand();
    DebugLogger.get()
        .logFile(
            getLabel(),
            "break pending crystal at "
                + crystal.blockPosition().toShortString()
                + " selfDmg="
                + selfDamage);
    lastBreakTick = tickCounter;
    return true;
  }

  private void tickPlace(Player target) {
    if (tickCounter - lastPlaceTick < placeDelay.getValue()) return;
    // One pending crystal at a time: break what's placed before placing more.
    // Dropped when it's gone or out of breaking reach: a stranded pending would
    // stall all future placements.
    if (pendingCrystal != null) {
      Vec3 pendingCenter = Vec3.atCenterOf(pendingCrystal);
      if (!hasLiveCrystal(pendingCrystal)
          || minecraft.player.distanceToSqr(
                  pendingCenter.x, pendingCenter.y, pendingCenter.z)
              > breakRange.getValue() * breakRange.getValue()) {
        pendingCrystal = null;
      } else {
        return;
      }
    }
    int crystalSlot =
        PlayerUtil.findInHotbar(stack -> stack.is(net.minecraft.world.item.Items.END_CRYSTAL));
    if (crystalSlot == -1) {
      if (!noCrystalLogged) {
        noCrystalLogged = true;
        DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "no crystals in hotbar");
      }
      return;
    }
    noCrystalLogged = false;

    BlockPos bestBase = null;
    float bestDamage = 0.0f;
    boolean bestVisible = false;
    BlockPos nearestWalled = null;
    double nearestWalledDist = Double.MAX_VALUE;
    Vec3 eye = minecraft.player.getEyePosition();
    double rangeSq = placeRange.getValue() * placeRange.getValue();
    BlockPos feet = target.blockPosition();
    int r = (int) Math.ceil(placeRange.getValue());
    for (int dx = -r; dx <= r; dx++) {
      for (int dy = -2; dy <= 2; dy++) {
        for (int dz = -r; dz <= r; dz++) {
          BlockPos base = feet.offset(dx, dy, dz);
          var state = minecraft.level.getBlockState(base);
          if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) continue;
          if (!minecraft.level.getBlockState(base.above()).isAir()) continue;
          if (eye.distanceToSqr(Vec3.atCenterOf(base)) > rangeSq) continue;
          Vec3 detonation =
              new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
          // Visibility ray goes to the crystal in open air, never into the base
          // block itself (a ray ending inside solid always reports BLOCKED).
          boolean visible =
              minecraft.level
                      .clip(
                          new net.minecraft.world.level.ClipContext(
                              eye,
                              detonation,
                              net.minecraft.world.level.ClipContext.Block.COLLIDER,
                              net.minecraft.world.level.ClipContext.Fluid.NONE,
                              minecraft.player))
                      .getType()
                  == net.minecraft.world.phys.HitResult.Type.MISS;
          double allowed = visible ? placeRange.getValue() : wallRange.getValue();
          double detDistSq = eye.distanceToSqr(detonation);
          if (detDistSq > allowed * allowed) {
            if (!visible
                && detDistSq < nearestWalledDist) {
              nearestWalledDist = detDistSq;
              nearestWalled = base.immutable();
            }
            continue;
          }
          if (hasCrystal(base)) continue;
          float targetDamage = damageAt(detonation, target);
          if (targetDamage < minDamage.getValue()) continue;
          if (damageAt(detonation, minecraft.player) > maxSelfDamage.getValue()) continue;
          if (targetDamage > bestDamage) {
            bestDamage = targetDamage;
            bestBase = base;
            bestVisible = visible;
          }
        }
      }
    }
    if (bestBase == null) {
      if (nearestWalled != null && !noWallLogged) {
        noWallLogged = true;
        DebugLogger.get()
            .log(
                getLabel(),
                DebugLogger.Level.WARN,
                "holding placement: nearest walled "
                    + nearestWalled.toShortString()
                    + " at "
                    + Math.sqrt(nearestWalledDist)
                    + "m");
      }
      return;
    }
    noWallLogged = false;

    int origSlot = minecraft.player.getInventory().getSelectedSlot();
    boolean needSwitch =
        autoSwitch.getValue() && crystalSlot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(crystalSlot);
      minecraft.player.getInventory().setSelectedSlot(crystalSlot);
    }
    final BlockPos placeAt = bestBase;
    PlayerUtil.sendSpoofTopUp();
    Runnable place =
        () -> {
          PlayerUtil.useItemOn(placeAt, Direction.UP);
          if (swingHand.getValue()) PlayerUtil.swingHand();
          if (needSwitch && switchBack.getValue()) PlayerUtil.swapBack();
        };
    if (rotate.getValue()) {
      PlayerUtil.withRotation(
          (float) PlayerUtil.getYaw(placeAt), (float) PlayerUtil.getPitch(placeAt), place);
    } else {
      place.run();
    }
    if (autoSwitch.getValue() && switchBack.getValue()) {
      minecraft.player.getInventory().setSelectedSlot(origSlot);
    }
    PlayerUtil.resyncSlot();
    DebugLogger.get()
        .logFile(
            getLabel(),
            "place crystal at "
                + placeAt.toShortString()
                + " targetDmg="
                + bestDamage
                + " visible="
                + bestVisible
                + " target="
                + target.getName().getString());
    pendingCrystal = placeAt.above().immutable();
    lastPlaceTick = tickCounter;
  }

  private boolean hasLiveCrystal(BlockPos crystalCell) {
    Vec3 center = Vec3.atCenterOf(crystalCell);
    for (Entity entity : minecraft.level.getEntities(null, targetBox(crystalCell.below()))) {
      if (entity instanceof EndCrystal
          && entity.isAlive()
          && entity.position().distanceToSqr(center) < 4.0) {
        return true;
      }
    }
    return false;
  }

  private boolean hasCrystal(BlockPos base) {
    Vec3 center = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
    for (Entity entity : minecraft.level.getEntities(null, targetBox(base))) {
      if (entity instanceof EndCrystal
          && entity.position().distanceToSqr(center) < 4.0) {
        return true;
      }
    }
    return false;
  }

  private static net.minecraft.world.phys.AABB targetBox(BlockPos base) {
    return new net.minecraft.world.phys.AABB(
        base.getX() - 1, base.getY(), base.getZ() - 1,
        base.getX() + 2, base.getY() + 3, base.getZ() + 2);
  }
}
