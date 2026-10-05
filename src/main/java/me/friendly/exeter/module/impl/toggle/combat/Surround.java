package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SearchSelectPopup;
import me.friendly.exeter.module.impl.toggle.render.clickgui.SelectionPopup;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.PopupProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Surrounds our own feet with blocks, AutoTrap-style but self-targeted. Height 2 walls the second
 * layer too; the head is never covered (no roof cell is ever placed). Off-center players get a 2x2
 * ring (4x4 safe area), and moving up more than a jump disables the module.
 */
public class Surround extends ToggleableModule {

  private final NumberProperty<Integer> height = new NumberProperty<Integer>(1, 1, 2, "Height");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 0.0, 10.0, "Place Range");
  private final NumberProperty<Integer> attemptDelay =
      new NumberProperty<Integer>(2, 0, 20, "Attempt Delay");
  private final NumberProperty<Integer> blocksPerTick =
      new NumberProperty<Integer>(1, 1, 8, "Blocks Per Tick");
  private final Property<Boolean> airPlace = new Property<Boolean>(true, "Air Place", "airplace");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> switchBack = new Property<Boolean>(true, "Switch Back");
  private final Property<Boolean> snapCenter =
      new Property<Boolean>(false, "Snap Center", "snapcenter");
  private final PopupProperty selectBlocks;
  private final SelectionPopup.Ids blockSelections = new SelectionPopup.Ids("Surround Blocks");

  private int tickCounter;
  private int lastAttemptTick;
  private boolean noBlockLogged;
  private double startY = Double.NaN;

  public Surround() {
    super("Surround", new String[] {"surround", "self-trap"}, 0x44DDFF, ModuleType.COMBAT);
    setDescription("Surrounds your feet with blocks, never covering your head.");
    this.selectBlocks = new PopupProperty("Select Blocks", this::openBlockPopup);
    blockSelections.getSelected().add("minecraft:obsidian");
    offerProperties(
        height,
        placeRange,
        attemptDelay,
        blocksPerTick,
        airPlace,
        snapCenter,
        rotate,
        swingHand,
        autoSwitch,
        switchBack,
        blockSelections.getProperty(),
        selectBlocks);
    this.listeners.add(
        new Listener<TickEvent>("surround_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });
    this.listeners.add(
        new Listener<PacketEvent>("surround_packet") {
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
    tickCounter = 0;
    lastAttemptTick = 0;
    noBlockLogged = false;
    startY = Double.NaN;
    if (snapCenter.getValue() && minecraft.player != null) {
      BlockPos feet = minecraft.player.blockPosition();
      minecraft.player.setPos(feet.getX() + 0.5, minecraft.player.getY(), feet.getZ() + 0.5);
      DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "snapped to block center");
    }
    DebugLogger.get()
        .log(getLabel(), DebugLogger.Level.INFO, "enabled height=" + height.getValue());
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    noBlockLogged = false;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
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
              String id =
                  net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
              String displayName =
                  net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
              items.add(SelectionPopup.idItem(id, displayName, blockSelections.getSelected()));
            });
    SelectionPopup.open("Surround Blocks", items, () -> blockSelections.save());
  }

  private void onTick() {
    tickCounter++;
    if (minecraft.player == null || minecraft.level == null) return;
    if (Double.isNaN(startY)) {
      startY = minecraft.player.getY();
    }
    // Any ascent means leaving: jumps, steps and pillars all toggle off immediately.
    if (minecraft.player.getY() > startY + 1e-3) {
      DebugLogger.get()
          .log(
              getLabel(),
              DebugLogger.Level.WARN,
              "moved up (y=" + minecraft.player.getY() + " startY=" + startY + "), disabling");
      toggle();
      return;
    }
    if (tickCounter - lastAttemptTick < attemptDelay.getValue()) return;

    int blockSlot = findBlockSlot();
    if (blockSlot == -1) {
      if (!noBlockLogged) {
        noBlockLogged = true;
        DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "no surround blocks in hotbar");
      }
      return;
    }
    noBlockLogged = false;

    BlockPos feet = minecraft.player.blockPosition();
    int placed = 0;
    for (BlockPos cell : surroundCells(feet)) {
      if (placed >= blocksPerTick.getValue()) break;
      if (!PlayerUtil.isAirOrReplaceable(cell)) continue;
      if (!PlayerUtil.inRange(cell, placeRange.getValue())) continue;
      BlockPos support = airPlace.getValue() ? cell : findSupport(cell, feet.getY());
      if (support == null) continue;
      placeCell(support, blockSlot);
      DebugLogger.get()
          .logFile(getLabel(), "place " + support.toShortString() + " slot=" + blockSlot);
      placed++;
    }
    if (placed > 0) {
      lastAttemptTick = tickCounter;
    }
  }

  /**
   * Ring around every column our bounding box touches: a centered player gets the normal 1-wide
   * ring, an off-center one a 2x2 ring (4x4 safe area). Height 2 adds the second layer. Never a
   * roof: the head stays open.
   */
  /**
   * Ring around every column we touch (2x2 when off-center, see PlayerUtil), stacked to height.
   * Never a roof: the head stays open.
   */
  private List<BlockPos> surroundCells(BlockPos feet) {
    List<BlockPos> cells = PlayerUtil.ringCells(minecraft.player, feet.getY(), height.getValue());
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

  /** Same pillar-climb as AutoTrap when air-place is off, grounded at our own feet. */
  private BlockPos findSupport(BlockPos cell, int groundY) {
    if (hasSupport(cell)) return cell;
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
          PlayerUtil.clickPlace(cell);
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

  private int findBlockSlot() {
    return PlayerUtil.findInHotbar(
        stack -> {
          if (!(stack.getItem() instanceof BlockItem)) return false;
          Block block = ((BlockItem) stack.getItem()).getBlock();
          String id =
              net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
          return blockSelections.getSelected().contains(id);
        });
  }
}
