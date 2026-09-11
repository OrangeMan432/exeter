package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.CrystalDamage;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class CrystalAura extends ToggleableModule {

  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(12.0, 1.0, 20.0, "Target Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Place Range");
  private final NumberProperty<Double> breakRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Break Range");
  private final NumberProperty<Integer> placeDelay =
      new NumberProperty<Integer>(2, 0, 20, "Place Delay");
  private final NumberProperty<Integer> breakDelay =
      new NumberProperty<Integer>(1, 0, 20, "Break Delay");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final NumberProperty<Double> lethalHealth =
      new NumberProperty<Double>(6.0, 0.0, 20.0, "Lethal Health");
  private final NumberProperty<Double> minDamage =
      new NumberProperty<Double>(6.0, 0.0, 20.0, "Min Damage");
  private final NumberProperty<Double> maxSelf =
      new NumberProperty<Double>(10.0, 0.0, 20.0, "Max Self");
  private final NumberProperty<Integer> burst =
      new NumberProperty<Integer>(1, 1, 5, "Burst Packets");
  private final NumberProperty<Integer> existedTicks =
      new NumberProperty<Integer>(0, 0, 20, "Exist Ticks");
  private final NumberProperty<Integer> maxAttacks =
      new NumberProperty<Integer>(2, 1, 10, "Max Attacks");
  private final Property<Boolean> setDead =
      new Property<Boolean>(true, "Set Dead");
  private final Property<Boolean> pauseEat =
      new Property<Boolean>(false, "Pause On Eat");

  private int tickCounter;
  private int lastPlaceTick = -100;
  private int lastBreakTick = -100;
  // BlackOut-pattern existence + inhibit tracking: id -> [firstSeenTick, attacks].
  private final Map<Integer, int[]> crystalStats = new HashMap<>();

  public CrystalAura() {
    super("CrystalAura", new String[] {"crystalaura", "crystal-aura"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Places and breaks end crystals around targets.");
    offerProperties(
        targetRange,
        placeRange,
        breakRange,
        placeDelay,
        breakDelay,
        rotate,
        swingHand,
        autoSwitch,
        lethalHealth,
        minDamage,
        maxSelf,
        burst,
        existedTicks,
        maxAttacks,
        setDead,
        pauseEat);
    this.listeners.add(
        new Listener<TickEvent>("crystalaura_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            CrystalAura.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
    lastPlaceTick = -100;
    lastBreakTick = -100;
    crystalStats.clear();
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    tickCounter++;
    boolean eating = pauseEat.getValue() && minecraft.player.isUsingItem();

    // Break first: crystals detonate the same tick they are attacked.
    if (!eating && tickCounter - lastBreakTick >= breakDelay.getValue()) {
      EndCrystal crystal = findCrystal();
      if (crystal != null) {
        attackCrystal(crystal);
        lastBreakTick = tickCounter;
        return;
      }
    }

    if (eating) return;
    Player target = findTarget();
    if (target == null) return;
    // Future-pattern Lethal: low-HP targets are one crystal from dead, skip the wait.
    boolean lethal = target.getHealth() <= lethalHealth.getValue().floatValue();
    if (!lethal && tickCounter - lastPlaceTick < placeDelay.getValue()) return;
    int crystalSlot = PlayerUtil.findInHotbar(s -> !s.isEmpty() && s.getItem() == Items.END_CRYSTAL);
    if (crystalSlot == -1) return;
    BlockPos base = findBase(target);
    if (base == null) return;
    placeCrystal(base, crystalSlot);
    lastPlaceTick = tickCounter;
  }

  private EndCrystal findCrystal() {
    double rangeSq = breakRange.getValue() * breakRange.getValue();
    EndCrystal best = null;
    double bestDist = Double.MAX_VALUE;
    for (Entity entity : minecraft.level.entitiesForRendering()) {
      if (!(entity instanceof EndCrystal crystal)) continue;
      if (!crystal.isAlive()) continue;
      double dist = minecraft.player.distanceToSqr(crystal);
      if (dist > rangeSq) continue;
      // BlackOut-pattern existence validation + inhibit cap.
      int[] stats = crystalStats.computeIfAbsent(crystal.getId(), k -> new int[] {tickCounter, 0});
      if (tickCounter - stats[0] < existedTicks.getValue()) continue;
      if (stats[1] >= maxAttacks.getValue()) continue;
      if (dist < bestDist) {
        bestDist = dist;
        best = crystal;
      }
    }
    return best;
  }

  private void attackCrystal(EndCrystal crystal) {
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      BlockPos pos = crystal.blockPosition();
      PlayerUtil.setRotation(PlayerUtil.getYaw(pos), PlayerUtil.getPitch(pos));
    }
    // BlackOut-pattern burst: several attack packets per crystal.
    for (int i = 0; i < burst.getValue(); i++) {
      minecraft.gameMode.attack(minecraft.player, crystal);
    }
    if (swingHand.getValue()) {
      minecraft.player.swing(InteractionHand.MAIN_HAND);
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    int[] stats = crystalStats.computeIfAbsent(crystal.getId(), k -> new int[] {tickCounter, 0});
    stats[1]++;
    // BlackOut-pattern despawn sim: stop rendering the dead crystal now.
    if (setDead.getValue()) {
      crystal.discard();
    }
  }

  private Player findTarget() {
    List<Player> players = new ArrayList<>();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player) continue;
      if (!entity.isAlive()) continue;
      if (minecraft.player.distanceTo(entity) > targetRange.getValue()) continue;
      players.add((Player) entity);
    }
    return players.stream()
        .min(Comparator.comparingDouble(minecraft.player::distanceTo))
        .orElse(null);
  }

  private BlockPos findBase(Player target) {
    double placeRangeSq = placeRange.getValue() * placeRange.getValue();
    BlockPos origin = target.blockPosition();
    boolean lethal = target.getHealth() <= lethalHealth.getValue().floatValue();
    // Gather in-range candidates first, damage-score only the closest few.
    List<BlockPos> nearby = new ArrayList<>();
    for (int x = -3; x <= 3; x++) {
      for (int y = -2; y <= 2; y++) {
        for (int z = -3; z <= 3; z++) {
          BlockPos base = origin.offset(x, y, z);
          if (!isObsidianBase(base)) continue;
          BlockPos crystalPos = base.above();
          if (!PlayerUtil.isAirOrReplaceable(crystalPos)) continue;
          if (!PlayerUtil.isAirOrReplaceable(crystalPos.above())) continue;
          if (!PlayerUtil.inRange(crystalPos, placeRange.getValue())) continue;
          if (minecraft.player.distanceToSqr(
                  crystalPos.getX() + 0.5, crystalPos.getY(), crystalPos.getZ() + 0.5)
              > placeRangeSq) continue;
          nearby.add(base);
        }
      }
    }
    nearby.sort(Comparator.comparingDouble(b -> b.distSqr(origin)));
    if (nearby.size() > 12) {
      nearby = nearby.subList(0, 12);
    }
    BlockPos best = null;
    double bestDamage = lethal ? -1.0 : minDamage.getValue();
    for (BlockPos base : nearby) {
      BlockPos crystalPos = base.above();
      Vec3 spawn =
          new Vec3(crystalPos.getX() + 0.5, crystalPos.getY(), crystalPos.getZ() + 0.5);
      double targetDamage = CrystalDamage.crystalDamage(target, spawn);
      if (!lethal && targetDamage < minDamage.getValue()) continue;
      double selfDamage = CrystalDamage.crystalDamage(minecraft.player, spawn);
      if (selfDamage > maxSelf.getValue()) continue;
      if (selfDamage + 0.5 >= minecraft.player.getHealth() + minecraft.player.getAbsorptionAmount()) {
        continue;
      }
      if (targetDamage > bestDamage) {
        bestDamage = targetDamage;
        best = base;
      }
    }
    return best;
  }

  private boolean isObsidianBase(BlockPos pos) {
    if (minecraft.level == null) return false;
    return minecraft.level.getBlockState(pos).getBlock() == Blocks.OBSIDIAN
        || minecraft.level.getBlockState(pos).getBlock() == Blocks.BEDROCK;
  }

  private void placeCrystal(BlockPos base, int slot) {
    boolean needSwitch =
        autoSwitch.getValue() && slot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(slot);
    }
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(base.above()), PlayerUtil.getPitch(base.above()));
    }
    PlayerUtil.useItemOn(base, Direction.UP);
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    if (needSwitch) {
      PlayerUtil.swapBack();
    }
  }

  private static boolean isObsidianItem(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == Blocks.OBSIDIAN;
  }
}
