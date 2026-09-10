package me.friendly.exeter.module.impl.toggle.combat;

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

public class Surround extends ToggleableModule {

  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> autoDisable =
      new Property<Boolean>(false, "Auto Disable");
  private final Property<Boolean> supportOnly =
      new Property<Boolean>(false, "Support Only");
  private final NumberProperty<Integer> blocksPerTick =
      new NumberProperty<Integer>(4, 1, 8, "Blocks Per Tick");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  private static final int[][] OFFSETS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

  public Surround() {
    super("Surround", new String[] {"surround", "self-trap-feet"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Places obsidian around your feet to block crystals.");
    offerProperties(rotate, autoSwitch, autoDisable, supportOnly, blocksPerTick, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("surround_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Surround.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    int obbySlot = PlayerUtil.findInHotbar(Surround::isObsidian);
    if (obbySlot == -1) return;

    BlockPos base = minecraft.player.blockPosition();
    int placed = 0;
    boolean allDone = true;
    for (int[] o : OFFSETS) {
      BlockPos pos = base.offset(o[0], 0, o[1]);
      if (!PlayerUtil.isAirOrReplaceable(pos)) continue;
      if (!PlayerUtil.isSolid(pos.below())) {
        allDone = false;
        continue;
      }
      allDone = false;
      if (placed >= blocksPerTick.getValue()) continue;
      placeObby(pos, obbySlot);
      placed++;
    }

    if (allDone && autoDisable.getValue() && !supportOnly.getValue()) {
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
