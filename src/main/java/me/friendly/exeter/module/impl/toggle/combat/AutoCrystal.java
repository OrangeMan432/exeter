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
 * Crystal aura in the spirit of 3arthh4ck's AutoCrystal, cut down to the working core: score
 * placements by raw explosion damage, place the best, break the best. No prediction, motion
 * extrapolation or multiplace.
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
  private final NumberProperty<Integer> predict = new NumberProperty<Integer>(0, 0, 20, "Predict");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> switchBack = new Property<Boolean>(true, "Switch Back");
  private final Property<Boolean> gappleSwap = new Property<Boolean>(true, "Gapple Swap");

  private int tickCounter;
  private int lastPlaceTick = -100;
  private int lastBreakTick = -100;
  private String lastTargetKey;
  private boolean noCrystalLogged;
  private boolean noWallLogged;
  private BlockPos pendingCrystal;
  private int pendingTick;
  private Vec3 prevTargetPos;
  private String prevTargetKey;
  private Vec3 lastPredicted;
  private Player renderTarget;
  private int eatSwapFrom = -1;
  private int eatGappleSlot = -1;
  private boolean noGappleLogged;
  private final java.util.ArrayDeque<Long> placeTimes = new java.util.ArrayDeque<>();

  public AutoCrystal() {
    super(
        "AutoCrystal", new String[] {"autocrystal", "crystal", "ca"}, 0xFF44FF, ModuleType.COMBAT);
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
        predict,
        rotate,
        swingHand,
        autoSwitch,
        switchBack,
        gappleSwap);
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
    this.listeners.add(
        new Listener<me.friendly.exeter.events.WorldRenderEvent>("autocrystal_render") {
          @Override
          public void call(me.friendly.exeter.events.WorldRenderEvent event) {
            onRender();
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
    prevTargetPos = null;
    prevTargetKey = null;
    renderTarget = null;
    lastPredicted = null;
    eatSwapFrom = -1;
    noGappleLogged = false;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    lastTargetKey = null;
    noCrystalLogged = false;
    noWallLogged = false;
    pendingCrystal = null;
    prevTargetPos = null;
    prevTargetKey = null;
    renderTarget = null;
    lastPredicted = null;
    eatSwapFrom = -1;
    noGappleLogged = false;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  /**
   * While the use key is held with crystals in hand, swaps to a gapple and holds all placing and
   * breaking so right-click eats instead of fighting the aura. Restores the crystal slot on
   * release, unless the player scrolled elsewhere meanwhile.
   *
   * @return true while placements and attacks must stay paused.
   */
  private boolean handleGappleSwap() {
    if (!gappleSwap.getValue()) {
      eatSwapFrom = -1;
      return false;
    }
    var inv = minecraft.player.getInventory();
    if (!minecraft.options.keyUse.isDown()) {
      if (eatSwapFrom != -1) {
        if (inv.getSelectedSlot() == eatGappleSlot) {
          inv.setSelectedSlot(eatSwapFrom);
          PlayerUtil.resyncSlot();
        }
        eatSwapFrom = -1;
      }
      return false;
    }
    var held = inv.getItem(inv.getSelectedSlot());
    boolean holdingCrystal = held.is(net.minecraft.world.item.Items.END_CRYSTAL);
    boolean holdingGapple =
        held.is(net.minecraft.world.item.Items.GOLDEN_APPLE)
            || held.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE);
    if (!holdingCrystal && !holdingGapple) return false;
    if (holdingCrystal && eatSwapFrom == -1) {
      // God apples first, regular gapples as fallback.
      int gappleSlot =
          PlayerUtil.findInHotbar(
              stack -> stack.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE));
      if (gappleSlot == -1) {
        gappleSlot =
            PlayerUtil.findInHotbar(stack -> stack.is(net.minecraft.world.item.Items.GOLDEN_APPLE));
      }
      if (gappleSlot == -1) {
        if (!noGappleLogged) {
          noGappleLogged = true;
          DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "no gapple in hotbar");
        }
      } else {
        noGappleLogged = false;
        eatSwapFrom = inv.getSelectedSlot();
        eatGappleSlot = gappleSlot;
        inv.setSelectedSlot(gappleSlot);
        PlayerUtil.resyncSlot();
        DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "swapped to gapple to eat");
      }
    }
    return true;
  }

  /** Live tag for the ArrayList: current target plus crystal placements in the last second. */
  @Override
  public String getTag() {
    if (lastTargetKey == null) return null;
    return lastTargetKey + ", " + placeRate() + "/s";
  }

  /** Placements in the trailing 1000ms window; pruned on every read so it stays live. */
  private int placeRate() {
    prunePlaceTimes();
    return placeTimes.size();
  }

  private void prunePlaceTimes() {
    long cutoff = System.currentTimeMillis() - 1000L;
    while (!placeTimes.isEmpty() && placeTimes.peekFirst() < cutoff) {
      placeTimes.pollFirst();
    }
  }

  private void onTick() {
    tickCounter++;
    if (minecraft.player == null || minecraft.level == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (handleGappleSwap()) return;
    Player target = findTarget();
    if (target == null) {
      lastTargetKey = null;
      pendingCrystal = null;
      renderTarget = null;
      lastPredicted = null;
      prevTargetPos = null;
      prevTargetKey = null;
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

    Vec3 predicted = predictedPos(target);
    renderTarget = target;
    lastPredicted = predicted;
    tickBreak(target, predicted);
    tickPlace(target, predicted);
  }

  /**
   * Linear extrapolation from per-tick position history. Remote velocity is ~zero except after
   * knockback (it isn't synced), so history is the only honest source. No gravity term: stair
   * descents already carry their downward velocity.
   */
  private Vec3 predictedPos(Player target) {
    Vec3 current = target.position();
    String key = target.getName().getString();
    Vec3 predicted = current;
    int ticks = predict.getValue();
    if (ticks > 0 && key.equals(prevTargetKey) && prevTargetPos != null) {
      Vec3 delta = current.subtract(prevTargetPos);
      // Teleports (ender pearls, chorus) aren't motion: reset instead of drawing
      // a line across the map.
      if (delta.lengthSqr() > 64.0) {
        prevTargetPos = null;
        prevTargetKey = null;
      } else {
        Vec3 velocity = delta;
        if (velocity.lengthSqr() > 0) {
          predicted = current.add(velocity.x * ticks, velocity.y * ticks, velocity.z * ticks);
        }
      }
    }
    prevTargetPos = current;
    prevTargetKey = key;
    if (ticks <= 0 || predicted == current) return current;
    return groundSnap(predicted, ticks);
  }

  /**
   * Drops (or lifts) the extrapolated point onto real terrain so the prediction follows staircases
   * instead of flying level past them. Needs headroom: the two cells above the ground must be free.
   */
  private Vec3 groundSnap(Vec3 predicted, int ticks) {
    if (minecraft.level == null) return predicted;
    int x = (int) Math.floor(predicted.x);
    int z = (int) Math.floor(predicted.z);
    int top = (int) Math.floor(predicted.y) + 2;
    int bottom = (int) Math.floor(predicted.y) - Math.max(2, ticks);
    for (int y = top; y >= bottom; y--) {
      BlockPos ground = new BlockPos(x, y, z);
      if (minecraft.level.getBlockState(ground).isAir()) continue;
      if (!minecraft.level.getBlockState(ground.above()).isAir()) continue;
      if (!minecraft.level.getBlockState(ground.above(2)).isAir()) continue;
      return new Vec3(predicted.x, y + 1.0, predicted.z);
    }
    return predicted;
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

  /** Same, but scored against the predicted position with the target's shifted box. */
  private float predictedDamageAt(Vec3 center, Player target, Vec3 predictedFeet) {
    Vec3 current = target.position();
    net.minecraft.world.phys.AABB box =
        target
            .getBoundingBox()
            .move(
                predictedFeet.x - current.x,
                predictedFeet.y - current.y,
                predictedFeet.z - current.z);
    return ExplosionUtil.explosionDamage(minecraft.level, center, 6.0F, target, predictedFeet, box);
  }

  private void onRender() {
    if (predict.getValue() <= 0) return;
    if (minecraft.level == null || minecraft.player == null) return;
    if (renderTarget == null || lastPredicted == null) return;
    if (!renderTarget.isAlive() || renderTarget.isRemoved()) {
      renderTarget = null;
      return;
    }
    Vec3 from = renderTarget.position().add(0, 1.0, 0);
    Vec3 to = lastPredicted.add(0, 1.0, 0);
    if (from.distanceToSqr(to) < 0.01) return;
    net.minecraft.gizmos.Gizmos.line(
            from, to, me.friendly.exeter.render.EspRenderManager.getClientColor(), 2.0f)
        .setAlwaysOnTop();
  }

  private void tickBreak(Player target, Vec3 predicted) {
    if (tickCounter - lastBreakTick < breakDelay.getValue()) return;
    if (breakPending()) return;
    EndCrystal best = null;
    float bestDamage = 0.0f;
    Vec3 eye = minecraft.player.getEyePosition();
    double rangeSq = breakRange.getValue() * breakRange.getValue();
    for (Entity entity : minecraft.level.getEntities(null, target.getBoundingBox().inflate(12.0))) {
      if (!(entity instanceof EndCrystal crystal)) continue;
      if (eye.distanceToSqr(crystal.position()) > rangeSq) continue;
      float targetDamage = predictedDamageAt(crystal.position(), target, predicted);
      if (targetDamage < minDamage.getValue()) continue;
      float rawSelf = damageAt(crystal.position(), minecraft.player);
      float selfDamage = ExplosionUtil.mitigatedExplosionDamage(minecraft.player, rawSelf);
      if (selfDamage > maxSelfDamage.getValue()
          || ExplosionUtil.wouldPopMitigated(minecraft.player, selfDamage)) continue;
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
   * Breaks our pending crystal first, ignoring target damage: the target may have left its blast
   * area, but it still blocks future placements. Returns true when a break was attempted (one break
   * per tick budget).
   */
  private boolean breakPending() {
    if (pendingCrystal == null) return false;
    Vec3 center = Vec3.atCenterOf(pendingCrystal);
    EndCrystal crystal = null;
    for (Entity entity : minecraft.level.getEntities(null, targetBox(pendingCrystal.below()))) {
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
    float rawSelf = damageAt(crystal.position(), minecraft.player);
    float selfDamage = ExplosionUtil.mitigatedExplosionDamage(minecraft.player, rawSelf);
    if (selfDamage > maxSelfDamage.getValue()
        || ExplosionUtil.wouldPopMitigated(minecraft.player, selfDamage)) {
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

  private void tickPlace(Player target, Vec3 predicted) {
    if (tickCounter - lastPlaceTick < placeDelay.getValue()) return;
    // One pending crystal at a time: break what's placed before placing more.
    // Dropped when it's gone or out of breaking reach: a stranded pending would
    // stall all future placements.
    if (pendingCrystal != null) {
      Vec3 pendingCenter = Vec3.atCenterOf(pendingCrystal);
      // Spawn latency grace: the entity needs a few ticks to show up client-side.
      boolean fresh = tickCounter - pendingTick < 5;
      if (!fresh
          && (!hasLiveCrystal(pendingCrystal)
              || minecraft.player.distanceToSqr(pendingCenter.x, pendingCenter.y, pendingCenter.z)
                  > breakRange.getValue() * breakRange.getValue())) {
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
    Vec3 bestDetonation = Vec3.ZERO;
    BlockPos nearestWalled = null;
    double nearestWalledDist = Double.MAX_VALUE;
    Vec3 eye = minecraft.player.getEyePosition();
    double rangeSq = placeRange.getValue() * placeRange.getValue();
    BlockPos feet =
        new BlockPos(
            (int) Math.floor(predicted.x),
            (int) Math.floor(predicted.y),
            (int) Math.floor(predicted.z));
    int r = (int) Math.ceil(placeRange.getValue());
    for (int dx = -r; dx <= r; dx++) {
      for (int dy = -2; dy <= 2; dy++) {
        for (int dz = -r; dz <= r; dz++) {
          BlockPos base = feet.offset(dx, dy, dz);
          var state = minecraft.level.getBlockState(base);
          if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) continue;
          if (!minecraft.level.getBlockState(base.above()).isAir()) continue;
          // Crystals can't be placed inside any entity, dropped items included.
          if (!minecraft
              .level
              .getEntities(null, new net.minecraft.world.phys.AABB(base.above()))
              .isEmpty()) {
            continue;
          }
          if (eye.distanceToSqr(Vec3.atCenterOf(base)) > rangeSq) continue;
          Vec3 detonation = new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
          // Visibility ray goes to the crystal in open air, never into the base
          // block itself (a ray ending inside solid always reports BLOCKED).
          boolean visible =
              minecraft
                      .level
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
            if (!visible && detDistSq < nearestWalledDist) {
              nearestWalledDist = detDistSq;
              nearestWalled = base.immutable();
            }
            continue;
          }
          if (hasCrystal(base)) continue;
          float targetDamage = predictedDamageAt(detonation, target, predicted);
          if (targetDamage < minDamage.getValue()) continue;
          float rawSelf = damageAt(detonation, minecraft.player);
          float selfDamage = ExplosionUtil.mitigatedExplosionDamage(minecraft.player, rawSelf);
          if (selfDamage > maxSelfDamage.getValue()
              || ExplosionUtil.wouldPopMitigated(minecraft.player, selfDamage)) continue;
          if (targetDamage > bestDamage) {
            bestDamage = targetDamage;
            bestBase = base;
            bestVisible = visible;
            bestDetonation = detonation;
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
                + " dist="
                + String.format(
                    "%.2f",
                    Math.sqrt(minecraft.player.getEyePosition().distanceToSqr(bestDetonation)))
                + " wallRange="
                + wallRange.getValue()
                + " target="
                + target.getName().getString());
    pendingCrystal = placeAt.above().immutable();
    pendingTick = tickCounter;
    lastPlaceTick = tickCounter;
    placeTimes.addLast(System.currentTimeMillis());
    prunePlaceTimes();
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
      if (entity instanceof EndCrystal && entity.position().distanceToSqr(center) < 4.0) {
        return true;
      }
    }
    return false;
  }

  private static net.minecraft.world.phys.AABB targetBox(BlockPos base) {
    return new net.minecraft.world.phys.AABB(
        base.getX() - 1,
        base.getY(),
        base.getZ() - 1,
        base.getX() + 2,
        base.getY() + 3,
        base.getZ() + 2);
  }
}
