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
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Lemon-pattern CevBreaker: packet-mines the cover above the target's head, then seats a
 * damage-scored crystal in whatever valid spot opens up. Self-validating: never places without an
 * obsidian/bedrock base.
 */
public class CevBreaker extends ToggleableModule {

  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(8.0, 1.0, 12.0, "Target Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Place Range");
  private final NumberProperty<Double> minDamage =
      new NumberProperty<Double>(5.0, 0.0, 20.0, "Min Damage");
  private final NumberProperty<Double> maxSelf =
      new NumberProperty<Double>(10.0, 0.0, 20.0, "Max Self");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> pauseEat = new Property<Boolean>(true, "Pause On Eat");

  public CevBreaker() {
    super("CevBreaker", new String[] {"cevbreaker", "cev-breaker"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Breaks head cover then crystals the opening.");
    offerProperties(
        targetRange, placeRange, minDamage, maxSelf, rotate, autoSwitch, swingHand, pauseEat);
    this.listeners.add(
        new Listener<TickEvent>("cevbreaker_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            CevBreaker.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (pauseEat.getValue() && minecraft.player.isUsingItem()) return;

    Player target = findTarget();
    if (target == null) return;

    // Break live crystals first so the opening detonates immediately.
    for (Entity entity : minecraft.level.entitiesForRendering()) {
      if (!(entity instanceof EndCrystal crystal) || !crystal.isAlive()) continue;
      if (minecraft.player.distanceTo(crystal) > placeRange.getValue()) continue;
      if (crystal.blockPosition().distSqr(target.blockPosition()) > 9) continue;
      attackCrystal(crystal);
      return;
    }

    BlockPos cover = target.blockPosition().above(2);
    BlockState coverState = minecraft.level.getBlockState(cover);
    if (!coverState.isAir()) {
      if (!isMineable(coverState)) return;
      if (!PlayerUtil.inRange(cover, placeRange.getValue())) return;
      mine(cover);
      return;
    }

    // Cover is open: seat a crystal wherever the base validates.
    int crystalSlot =
        PlayerUtil.findInHotbar(s -> !s.isEmpty() && s.getItem() == Items.END_CRYSTAL);
    if (crystalSlot == -1) return;
    BlockPos base = findValidBase(target);
    if (base == null) return;
    Vec3 spawn = new Vec3(base.getX() + 0.5, base.above().getY(), base.getZ() + 0.5);
    double targetDamage = CrystalDamage.crystalDamage(target, spawn);
    if (targetDamage < minDamage.getValue()) return;
    double selfDamage = CrystalDamage.crystalDamage(minecraft.player, spawn);
    if (selfDamage > maxSelf.getValue()) return;
    if (selfDamage + 0.5 >= minecraft.player.getHealth() + minecraft.player.getAbsorptionAmount())
      return;
    click(base, Direction.UP, crystalSlot);
  }

  private BlockPos findValidBase(Player target) {
    BlockPos origin = target.blockPosition();
    BlockPos[] candidates = {
      origin.above(),
      origin,
      origin.above(2),
      origin.north(),
      origin.south(),
      origin.east(),
      origin.west()
    };
    BlockPos best = null;
    double bestScore = Double.MAX_VALUE;
    for (BlockPos air : candidates) {
      if (!PlayerUtil.isAirOrReplaceable(air)) continue;
      if (!PlayerUtil.isAirOrReplaceable(air.above())) continue;
      BlockState below = minecraft.level.getBlockState(air.below());
      if (below.getBlock() != Blocks.OBSIDIAN && below.getBlock() != Blocks.BEDROCK) continue;
      if (!PlayerUtil.inRange(air, placeRange.getValue())) continue;
      double score = air.distSqr(origin);
      if (score < bestScore) {
        bestScore = score;
        best = air.below();
      }
    }
    return best;
  }

  private Player findTarget() {
    Player best = null;
    double bestDist = targetRange.getValue();
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

  private void mine(BlockPos pos) {
    swapToBestTool(pos);
    minecraft
        .getConnection()
        .send(
            new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
    minecraft
        .getConnection()
        .send(
            new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
  }

  private void swapToBestTool(BlockPos pos) {
    BlockState state = minecraft.level.getBlockState(pos);
    int best = -1;
    float bestSpeed = 1.0f;
    for (int i = 0; i < 9; i++) {
      ItemStack stack = minecraft.player.getInventory().getItem(i);
      if (stack.isEmpty()) continue;
      float speed = stack.getDestroySpeed(state);
      if (speed > bestSpeed) {
        bestSpeed = speed;
        best = i;
      }
    }
    if (best != -1
        && autoSwitch.getValue()
        && best != minecraft.player.getInventory().getSelectedSlot()) {
      PlayerUtil.swapTo(best);
    }
  }

  private void attackCrystal(EndCrystal crystal) {
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      BlockPos pos = crystal.blockPosition();
      PlayerUtil.setRotation(PlayerUtil.getYaw(pos), PlayerUtil.getPitch(pos));
    }
    minecraft.gameMode.attack(minecraft.player, crystal);
    if (swingHand.getValue()) {
      minecraft.player.swing(InteractionHand.MAIN_HAND);
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
  }

  private void click(BlockPos base, Direction face, int slot) {
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
    PlayerUtil.useItemOn(base, face);
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

  private static boolean isMineable(BlockState state) {
    return state.getBlock() != Blocks.BEDROCK
        && state.getBlock() != Blocks.REINFORCED_DEEPSLATE
        && state.getBlock() != Blocks.END_PORTAL_FRAME
        && !state.isAir();
  }
}
