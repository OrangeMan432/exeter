package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Shoreline-pattern BlockLag: buries an obsidian block at your feet and
 * centers you into it so the server rubberbands you inside.
 */
public class BlockLag extends ToggleableModule {

  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> autoDisable =
      new Property<Boolean>(true, "Auto Disable");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  public BlockLag() {
    super("BlockLag", new String[] {"blocklag", "block-lag"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Rubberbands you inside an obsidian block.");
    offerProperties(rotate, autoSwitch, autoDisable, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("blocklag_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            BlockLag.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    BlockPos feet = minecraft.player.blockPosition();
    if (!PlayerUtil.isAirOrReplaceable(feet)) {
      if (autoDisable.getValue()) {
        setRunning(false);
      }
      return;
    }

    int obbySlot = PlayerUtil.findInHotbar(BlockLag::isObsidian);
    if (obbySlot == -1) return;

    boolean needSwitch =
        autoSwitch.getValue() && obbySlot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(obbySlot);
    }
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(feet), PlayerUtil.getPitch(feet));
    }
    // Click the floor so the block lands exactly at our feet.
    PlayerUtil.useItemOn(feet.below(), Direction.UP);
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    if (needSwitch) {
      PlayerUtil.swapBack();
    }

    // Center into the fresh block.
    double x = Math.floor(minecraft.player.getX()) + 0.5;
    double z = Math.floor(minecraft.player.getZ()) + 0.5;
    minecraft.player.setDeltaMovement(
        (x - minecraft.player.getX()) / 2.0,
        minecraft.player.getDeltaMovement().y,
        (z - minecraft.player.getZ()) / 2.0);

    if (autoDisable.getValue()) {
      setRunning(false);
    }
  }

  private static boolean isObsidian(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == Blocks.OBSIDIAN;
  }
}
