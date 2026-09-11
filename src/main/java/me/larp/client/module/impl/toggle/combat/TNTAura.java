package me.larp.client.module.impl.toggle.combat;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Lemon-pattern TNTAura: seats TNT by the target and lights it with
 * flint and steel. Primed TNT needs no line of sight to hurt.
 */
public class TNTAura extends ToggleableModule {

  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(8.0, 1.0, 12.0, "Target Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Place Range");
  private final NumberProperty<Integer> placeDelay =
      new NumberProperty<Integer>(10, 0, 40, "Place Delay");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  private int tickCounter;
  private int lastPlaceTick = -100;

  public TNTAura() {
    super("TNTAura", new String[] {"tntaura", "tnt-aura"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("TNT traps for targets.");
    offerProperties(targetRange, placeRange, placeDelay, rotate, autoSwitch, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("tntaura_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            TNTAura.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
    lastPlaceTick = -100;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    tickCounter++;
    if (tickCounter - lastPlaceTick < placeDelay.getValue()) return;

    Player target = findTarget();
    if (target == null) return;
    int tntSlot = PlayerUtil.findInHotbar(TNTAura::isTNT);
    if (tntSlot == -1) return;
    int steelSlot = PlayerUtil.findInHotbar(
        s -> !s.isEmpty() && s.getItem() == Items.FLINT_AND_STEEL);
    if (steelSlot == -1) return;

    BlockPos spot = findSpot(target);
    if (spot == null) return;

    place(spot, tntSlot);
    ignite(spot, steelSlot);
    lastPlaceTick = tickCounter;
  }

  private BlockPos findSpot(Player target) {
    BlockPos origin = target.blockPosition();
    BlockPos best = null;
    double bestScore = Double.MAX_VALUE;
    for (int x = -2; x <= 2; x++) {
      for (int y = -1; y <= 1; y++) {
        for (int z = -2; z <= 2; z++) {
          BlockPos pos = origin.offset(x, y, z);
          if (pos.equals(origin) || pos.equals(origin.above())) continue;
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

  private void place(BlockPos pos, int slot) {
    swap(slot);
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(pos), PlayerUtil.getPitch(pos));
    }
    clickNeighbor(pos);
    swing();
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    unswap(slot);
  }

  private void ignite(BlockPos pos, int slot) {
    swap(slot);
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(pos), PlayerUtil.getPitch(pos));
    }
    // Click the placed TNT directly to light it.
    PlayerUtil.useItemOn(pos, Direction.UP);
    swing();
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    unswap(slot);
  }

  private void swap(int slot) {
    if (autoSwitch.getValue() && slot != minecraft.player.getInventory().getSelectedSlot()) {
      PlayerUtil.swapTo(slot);
    }
  }

  private void unswap(int slot) {
    if (autoSwitch.getValue() && slot != minecraft.player.getInventory().getSelectedSlot()) {
      PlayerUtil.swapBack();
    }
  }

  private void swing() {
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
  }

  private void clickNeighbor(BlockPos pos) {
    for (Direction dir : Direction.values()) {
      BlockPos neighbor = pos.relative(dir);
      if (PlayerUtil.isSolid(neighbor)) {
        PlayerUtil.useItemOn(neighbor, dir.getOpposite());
        return;
      }
    }
    PlayerUtil.useItemOn(pos.below(), Direction.UP);
  }

  private static boolean isTNT(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == Blocks.TNT;
  }
}
