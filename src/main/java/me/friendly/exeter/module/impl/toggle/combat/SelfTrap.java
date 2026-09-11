package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Fully surrounds yourself: feet ring, head ring, and top cover. */
public class SelfTrap extends ToggleableModule {

  private final NumberProperty<Integer> blocksPerTick =
      new NumberProperty<Integer>(4, 1, 8, "Blocks Per Tick");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Place Range");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> top = new Property<Boolean>(true, "Top");
  private final Property<Boolean> autoDisable =
      new Property<Boolean>(false, "Auto Disable");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  public SelfTrap() {
    super("SelfTrap", new String[] {"selftrap", "self-trap"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Fully cages yourself in obsidian.");
    offerProperties(blocksPerTick, placeRange, rotate, autoSwitch, top, autoDisable, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("selftrap_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            SelfTrap.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    int obbySlot = PlayerUtil.findInHotbar(SelfTrap::isObsidian);
    if (obbySlot == -1) return;

    BlockPos feet = minecraft.player.blockPosition();
    BlockPos head = feet.above();
    List<BlockPos> cage = new ArrayList<>();
    cage.add(feet.north());
    cage.add(feet.south());
    cage.add(feet.east());
    cage.add(feet.west());
    cage.add(head.north());
    cage.add(head.south());
    cage.add(head.east());
    cage.add(head.west());
    if (top.getValue()) {
      cage.add(head.above());
    }
    cage.sort(Comparator.comparingDouble(p -> p.distSqr(feet)));

    boolean allDone = true;
    int placed = 0;
    for (BlockPos pos : cage) {
      if (!PlayerUtil.isAirOrReplaceable(pos)) continue;
      if (!PlayerUtil.inRange(pos, placeRange.getValue())) {
        allDone = false;
        continue;
      }
      allDone = false;
      if (placed >= blocksPerTick.getValue()) continue;
      placeObby(pos, obbySlot);
      placed++;
    }

    if (allDone && autoDisable.getValue()) {
      setRunning(false);
    }
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
    PlayerUtil.useItemOn(pos.below(), Direction.UP);
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

  private static boolean isObsidian(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == Blocks.OBSIDIAN;
  }
}
