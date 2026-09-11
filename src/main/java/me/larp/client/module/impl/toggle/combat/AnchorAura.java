package me.larp.client.module.impl.toggle.combat;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.CrystalDamage;
import me.larp.client.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * BlackOut-pattern AnchorAura: place anchor, charge with glowstone, detonate. Damage-gated on both
 * place and explode like the original.
 */
public class AnchorAura extends ToggleableModule {

  private final NumberProperty<Double> targetDistance =
      new NumberProperty<Double>(10.0, 1.0, 15.0, "Target Distance");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Place Range");
  private final NumberProperty<Integer> charges = new NumberProperty<Integer>(2, 1, 4, "Charges");
  private final NumberProperty<Integer> placeDelay =
      new NumberProperty<Integer>(4, 0, 20, "Place Delay");
  private final NumberProperty<Integer> actionDelay =
      new NumberProperty<Integer>(2, 0, 20, "Action Delay");
  private final NumberProperty<Double> minPlace =
      new NumberProperty<Double>(5.0, 0.0, 20.0, "Min Place");
  private final NumberProperty<Double> maxSelfPlace =
      new NumberProperty<Double>(10.0, 0.0, 20.0, "Max Self Place");
  private final NumberProperty<Double> minExplode =
      new NumberProperty<Double>(5.0, 0.0, 20.0, "Min Explode");
  private final NumberProperty<Double> maxSelfExplode =
      new NumberProperty<Double>(10.0, 0.0, 20.0, "Max Self Explode");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  private int tickCounter;
  private int lastPlaceTick = -100;
  private int lastActionTick = -100;

  public AnchorAura() {
    super("AnchorAura", new String[] {"anchoraura", "anchor-aura"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Anchor place, charge, and detonate engine.");
    offerProperties(
        targetDistance,
        placeRange,
        charges,
        placeDelay,
        actionDelay,
        minPlace,
        maxSelfPlace,
        minExplode,
        maxSelfExplode,
        rotate,
        autoSwitch,
        swingHand);
    this.listeners.add(
        new Listener<TickEvent>("anchoraura_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AnchorAura.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
    lastPlaceTick = -100;
    lastActionTick = -100;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    // Anchors only explode outside the Nether.
    if (minecraft.level.dimension() == net.minecraft.world.level.Level.NETHER) return;
    tickCounter++;

    Player target = findTarget();
    if (target == null) return;

    BlockPos anchor = findAnchor(target);
    if (anchor == null) {
      tryPlace(target);
      return;
    }

    int charge = chargeOf(anchor);
    if (charge < charges.getValue()) {
      tryCharge(anchor);
      return;
    }
    tryExplode(anchor, target);
  }

  private Player findTarget() {
    Player best = null;
    double bestDist = targetDistance.getValue();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player || !entity.isAlive()) continue;
      double d = minecraft.player.distanceTo(entity);
      if (d < bestDist) {
        bestDist = d;
        best = (Player) entity;
      }
    }
    return best;
  }

  private BlockPos findAnchor(Player target) {
    BlockPos origin = target.blockPosition();
    BlockPos best = null;
    double bestDist = Double.MAX_VALUE;
    for (int x = -3; x <= 3; x++) {
      for (int y = -2; y <= 2; y++) {
        for (int z = -3; z <= 3; z++) {
          BlockPos pos = origin.offset(x, y, z);
          BlockState state = minecraft.level.getBlockState(pos);
          if (state.getBlock() != Blocks.RESPAWN_ANCHOR) continue;
          if (!PlayerUtil.inRange(pos, placeRange.getValue())) continue;
          double d = pos.distSqr(origin);
          if (d < bestDist) {
            bestDist = d;
            best = pos;
          }
        }
      }
    }
    return best;
  }

  private void tryPlace(Player target) {
    if (tickCounter - lastPlaceTick < placeDelay.getValue()) return;
    int anchorSlot = PlayerUtil.findInHotbar(AnchorAura::isAnchor);
    if (anchorSlot == -1) return;
    BlockPos spot = findSpot(target);
    if (spot == null) return;
    Vec3 boom = centerOf(spot);
    double targetDamage = CrystalDamage.explosionDamage(target, target.getBoundingBox(), boom, 5.0);
    if (targetDamage < minPlace.getValue()) return;
    double selfDamage =
        CrystalDamage.explosionDamage(
            minecraft.player, minecraft.player.getBoundingBox(), boom, 5.0);
    if (selfDamage > maxSelfPlace.getValue()) return;
    if (selfDamage + 0.5 >= minecraft.player.getHealth() + minecraft.player.getAbsorptionAmount())
      return;
    click(spot.below(), Direction.UP, anchorSlot);
    lastPlaceTick = tickCounter;
  }

  private BlockPos findSpot(Player target) {
    BlockPos origin = target.blockPosition();
    BlockPos best = null;
    double bestScore = Double.MAX_VALUE;
    for (int x = -3; x <= 3; x++) {
      for (int y = -2; y <= 2; y++) {
        for (int z = -3; z <= 3; z++) {
          BlockPos pos = origin.offset(x, y, z);
          if (!PlayerUtil.isAirOrReplaceable(pos)) continue;
          if (!PlayerUtil.isSolid(pos.below())) continue;
          if (!PlayerUtil.inRange(pos, placeRange.getValue())) continue;
          double score = pos.distSqr(origin);
          if (score < bestScore) {
            bestScore = score;
            best = pos;
          }
        }
      }
    }
    return best;
  }

  private void tryCharge(BlockPos anchor) {
    if (tickCounter - lastActionTick < actionDelay.getValue()) return;
    int glowSlot =
        PlayerUtil.findInHotbar(s -> !s.isEmpty() && s.getItem() == Items.GLOWSTONE_DUST);
    if (glowSlot == -1) {
      glowSlot = PlayerUtil.findInHotbar(AnchorAura::isGlowstoneBlock);
      if (glowSlot == -1) return;
    }
    click(anchor, Direction.UP, glowSlot);
    lastActionTick = tickCounter;
  }

  private void tryExplode(BlockPos anchor, Player target) {
    if (tickCounter - lastActionTick < actionDelay.getValue()) return;
    Vec3 boom = centerOf(anchor);
    double targetDamage = CrystalDamage.explosionDamage(target, target.getBoundingBox(), boom, 5.0);
    if (targetDamage < minExplode.getValue()) return;
    double selfDamage =
        CrystalDamage.explosionDamage(
            minecraft.player, minecraft.player.getBoundingBox(), boom, 5.0);
    if (selfDamage > maxSelfExplode.getValue()) return;
    if (selfDamage + 0.5 >= minecraft.player.getHealth() + minecraft.player.getAbsorptionAmount())
      return;
    // Explode with an empty hand so the click detonates instead of charging.
    click(anchor, Direction.UP, findEmptySlot());
    lastActionTick = tickCounter;
  }

  private int chargeOf(BlockPos pos) {
    BlockState state = minecraft.level.getBlockState(pos);
    if (state.getBlock() != Blocks.RESPAWN_ANCHOR) return 0;
    try {
      return state.getValue(RespawnAnchorBlock.CHARGE);
    } catch (Exception e) {
      return 0;
    }
  }

  private Vec3 centerOf(BlockPos pos) {
    return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
  }

  private int findEmptySlot() {
    for (int i = 0; i < 9; i++) {
      if (minecraft.player.getInventory().getItem(i).isEmpty()) return i;
    }
    return minecraft.player.getInventory().getSelectedSlot();
  }

  private void click(BlockPos pos, Direction face, int slot) {
    boolean needSwitch =
        autoSwitch.getValue() && slot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(slot);
    }
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(pos), PlayerUtil.getPitch(pos));
    }
    PlayerUtil.useItemOn(pos, face);
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

  private static boolean isAnchor(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == Blocks.RESPAWN_ANCHOR;
  }

  private static boolean isGlowstoneBlock(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == Blocks.GLOWSTONE;
  }
}
