package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SearchSelectPopup;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SelectionPopup;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Surrounds players with obsidian (or popup-selected blocks). Full mode cages feet,
 * head and top; Head mode only covers above the head. With AirPlace off every cell
 * needs a solid neighbour, so supports are pillared up from the ground first.
 */
public class AutoTrap extends ToggleableModule {

  public enum TrapMode {
    FULL,
    HEAD
  }

  private final EnumProperty<TrapMode> mode = new EnumProperty<>(TrapMode.FULL, "Mode", "mode");
  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(6.0, 0.0, 12.0, "Target Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 0.0, 10.0, "Place Range");
  private final NumberProperty<Integer> attemptDelay =
      new NumberProperty<Integer>(2, 0, 20, "Attempt Delay");
  private final Property<Boolean> airPlace = new Property<Boolean>(true, "Air Place", "airplace");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> switchBack = new Property<Boolean>(true, "Switch Back");
  private final PopupProperty selectBlocks;
  private final SelectionPopup.Ids blockSelections = new SelectionPopup.Ids("Trap Blocks");

  private int tickCounter;
  private int lastAttemptTick;

  public AutoTrap() {
    super("AutoTrap", new String[] {"autotrap", "trap"}, 0x8844FF, ModuleType.COMBAT);
    setDescription("Traps players in obsidian.");
    this.selectBlocks = new PopupProperty("Select Blocks", this::openBlockPopup);
    blockSelections.getSelected().add("minecraft:obsidian");
    offerProperties(
        mode,
        targetRange,
        placeRange,
        attemptDelay,
        airPlace,
        rotate,
        swingHand,
        autoSwitch,
        switchBack,
        blockSelections.getProperty(),
        selectBlocks);
    this.listeners.add(
        new Listener<TickEvent>("autotrap_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });
    this.listeners.add(
        new Listener<PacketEvent>("autotrap_packet") {
          @Override
          public void call(PacketEvent event) {
            PlayerUtil.spoofMovement(event);
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    blockSelections.load();
    if (blockSelections.getSelected().isEmpty()) {
      blockSelections.getSelected().add("minecraft:obsidian");
    }
  }

  private void openBlockPopup() {
    List<SearchSelectPopup.ToggleItem> items = new ArrayList<>();
    net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream()
        .filter(block -> block.asItem() instanceof BlockItem)
        .filter(block -> block != Blocks.AIR && block != Blocks.WATER && block != Blocks.LAVA)
        .sorted(
            (a, b) -> {
              String aName =
                  net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(a).getPath();
              String bName =
                  net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath();
              return aName.compareTo(bName);
            })
        .forEach(
            block -> {
              String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
              String displayName =
                  net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
              items.add(SelectionPopup.idItem(id, displayName, blockSelections.getSelected()));
            });
    SelectionPopup.open("Trap Blocks", items, () -> blockSelections.save());
  }

  private void onTick() {
    tickCounter++;
    if (minecraft.player == null || minecraft.level == null) return;
    if (tickCounter - lastAttemptTick < attemptDelay.getValue()) return;

    Player target = findTarget();
    if (target == null) return;
    int blockSlot = findBlockSlot();
    if (blockSlot == -1) return;

    for (BlockPos cell : trapCells(target)) {
      if (!PlayerUtil.isAirOrReplaceable(cell)) continue;
      if (!PlayerUtil.inRange(cell, placeRange.getValue())) continue;
      BlockPos support = airPlace.getValue() ? cell : findSupport(cell, target);
      if (support == null) continue;
      placeCell(support, blockSlot);
      lastAttemptTick = tickCounter;
      return;
    }
  }

  private List<BlockPos> trapCells(Player target) {
    BlockPos feet = target.blockPosition();
    List<BlockPos> cells = new ArrayList<>();
    if (mode.getValue() == TrapMode.HEAD) {
      cells.add(feet.above(2));
      return cells;
    }
    for (Direction dir : Direction.Plane.HORIZONTAL) {
      cells.add(feet.relative(dir));
      cells.add(feet.above(1).relative(dir));
    }
    cells.add(feet.above(2));
    // Closest first so the trap closes inward.
    cells.sort(
        Comparator.comparingDouble(
            pos ->
                minecraft.player.distanceToSqr(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));
    return cells;
  }

  private boolean hasSupport(BlockPos cell) {
    for (Direction dir : Direction.values()) {
      if (!PlayerUtil.isAirOrReplaceable(cell.relative(dir))) return true;
    }
    return false;
  }

  /**
   * Finds a support to place first when air-place is off: climbs a single adjacent
   * column from the ground, returning its lowest missing cell so the pillar towers
   * before the cover goes on. Returns the cell itself when supported, null when no
   * adjacent column can reach it.
   */
  private BlockPos findSupport(BlockPos cell, Player target) {
    if (hasSupport(cell)) return cell;
    int groundY = target.blockPosition().getY();
    for (Direction dir : Direction.Plane.HORIZONTAL) {
      BlockPos base =
          new BlockPos(cell.getX() + dir.getStepX(), groundY, cell.getZ() + dir.getStepZ());
      BlockPos candidate = null;
      boolean columnOk = true;
      for (int y = groundY; y <= cell.getY(); y++) {
        BlockPos pillar = new BlockPos(base.getX(), y, base.getZ());
        if (!PlayerUtil.isAirOrReplaceable(pillar)) {
          continue;
        }
        if (!PlayerUtil.inRange(pillar, placeRange.getValue())) {
          columnOk = false;
          break;
        }
        if (candidate == null) {
          candidate = pillar;
        }
      }
      if (columnOk && candidate != null) return candidate;
    }
    return null;
  }

  private void placeCell(BlockPos cell, int blockSlot) {
    int origSlot = minecraft.player.getInventory().getSelectedSlot();
    boolean needSwitch =
        autoSwitch.getValue() && blockSlot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(blockSlot);
      minecraft.player.getInventory().setSelectedSlot(blockSlot);
    }
    PlayerUtil.sendSpoofTopUp();
    Runnable place =
        () -> {
          clickPlace(cell);
          if (swingHand.getValue()) PlayerUtil.swingHand();
          if (needSwitch && switchBack.getValue()) PlayerUtil.swapBack();
        };
    if (rotate.getValue()) {
      PlayerUtil.withRotation(
          (float) PlayerUtil.getYaw(cell), (float) PlayerUtil.getPitch(cell), place);
    } else {
      place.run();
    }
    if (autoSwitch.getValue() && switchBack.getValue()) {
      minecraft.player.getInventory().setSelectedSlot(origSlot);
    }
    PlayerUtil.resyncSlot();
  }

  private void clickPlace(BlockPos cell) {
    for (Direction dir : Direction.values()) {
      BlockPos neighbour = cell.relative(dir);
      var state = minecraft.level.getBlockState(neighbour);
      if (state.isAir() || state.canBeReplaced()) continue;
      PlayerUtil.useItemOn(neighbour, dir.getOpposite());
      return;
    }
    PlayerUtil.useItemOn(cell, Direction.UP);
  }

  private int findBlockSlot() {
    return PlayerUtil.findInHotbar(
        stack -> {
          if (!(stack.getItem() instanceof BlockItem)) return false;
          Block block = ((BlockItem) stack.getItem()).getBlock();
          String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
          return blockSelections.getSelected().contains(id);
        });
  }

  private Player findTarget() {
    List<Player> players = new ArrayList<>();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player) continue;
      if (!entity.isAlive()) continue;
      if (!(entity instanceof Player)) continue;
      if (!Exeter.getInstance().getFriendManager().isTargetable(entity.getName().getString())) {
        continue;
      }
      double distance = minecraft.player.distanceTo(entity);
      if (distance <= targetRange.getValue()) {
        players.add((Player) entity);
      }
    }
    return players.stream()
        .min(Comparator.comparingDouble(p -> minecraft.player.distanceTo(p)))
        .orElse(null);
  }
}
