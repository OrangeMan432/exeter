package me.larp.client.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Shoreline-pattern HoleFill: fills air holes near enemies so they cannot retreat into them.
 * Proximity mode only fills holes the enemy can reach.
 */
public class HoleFill extends ToggleableModule {

  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(4.0, 0.0, 6.0, "Place Range");
  private final NumberProperty<Double> enemyRange =
      new NumberProperty<Double>(10.0, 0.0, 15.0, "Enemy Range");
  private final Property<Boolean> proximity = new Property<Boolean>(true, "Proximity Check");
  private final NumberProperty<Double> proximityRange =
      new NumberProperty<Double>(2.0, 0.0, 5.0, "Proximity Range");
  private final NumberProperty<Integer> blocksPerTick =
      new NumberProperty<Integer>(2, 1, 5, "Blocks Per Tick");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> autoDisable = new Property<Boolean>(false, "Auto Disable");

  private List<BlockPos> cached = new ArrayList<>();
  private int tickCounter;

  @Override
  protected void onEnable() {
    super.onEnable();
    cached = new ArrayList<>();
    tickCounter = 0;
  }

  public HoleFill() {
    super("HoleFill", new String[] {"holefill", "hole-fill"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Fills holes near enemies with obsidian.");
    offerProperties(
        placeRange,
        enemyRange,
        proximity,
        proximityRange,
        blocksPerTick,
        rotate,
        autoSwitch,
        autoDisable);
    this.listeners.add(
        new Listener<TickEvent>("holefill_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            HoleFill.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    int obbySlot = PlayerUtil.findInHotbar(HoleFill::isObsidian);
    if (obbySlot == -1) return;

    // Scan every 5 ticks; place from cache every tick. Prune filled slots first.
    cached.removeIf(pos -> !PlayerUtil.isAirOrReplaceable(pos));
    if (tickCounter++ % 5 == 0 || cached.isEmpty()) {
      List<BlockPos> fills = new ArrayList<>();
      BlockPos origin = minecraft.player.blockPosition();
      int r = (int) Math.ceil(placeRange.getValue()) + 1;
      for (int x = -r; x <= r; x++) {
        for (int y = -2; y <= 1; y++) {
          for (int z = -r; z <= r; z++) {
            BlockPos pos = origin.offset(x, y, z);
            if (!isHole(pos)) continue;
            if (!PlayerUtil.inRange(pos, placeRange.getValue())) continue;
            if (proximity.getValue() && !nearEnemy(pos)) continue;
            fills.add(pos);
          }
        }
      }
      fills.sort(Comparator.comparingDouble(p -> p.distSqr(origin)));
      cached = fills;
    }

    if (cached.isEmpty()) {
      if (autoDisable.getValue()) {
        setRunning(false);
      }
      return;
    }

    int placed = 0;
    for (BlockPos pos : cached) {
      if (placed >= blocksPerTick.getValue()) break;
      if (!PlayerUtil.isAirOrReplaceable(pos)) continue;
      placeObby(pos, obbySlot);
      placed++;
    }
  }

  private boolean nearEnemy(BlockPos hole) {
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player || !entity.isAlive()) continue;
      if (minecraft.player.distanceTo(entity) > enemyRange.getValue()) continue;
      double dx = entity.getX() - (hole.getX() + 0.5);
      double dz = entity.getZ() - (hole.getZ() + 0.5);
      if (dx * dx + dz * dz <= proximityRange.getValue() * proximityRange.getValue()) {
        return true;
      }
    }
    return false;
  }

  private boolean isHole(BlockPos pos) {
    if (!PlayerUtil.isAirOrReplaceable(pos)) return false;
    if (!PlayerUtil.isSolid(pos.below())) return false;
    // Skip holes the player is standing in.
    if (pos.equals(minecraft.player.blockPosition())) return false;
    return true;
  }

  private void placeObby(BlockPos pos, int slot) {
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
    clickNeighbor(pos);
    PlayerUtil.swingHand();
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    if (needSwitch) {
      PlayerUtil.swapBack();
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

  private static boolean isObsidian(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == Blocks.OBSIDIAN;
  }
}
