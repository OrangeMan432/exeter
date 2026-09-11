package me.larp.client.module.impl.toggle.movement;

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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Bridges and towers: seats blocks under you as you move. */
public class Scaffold extends ToggleableModule {

  private final NumberProperty<Integer> blocksPerTick =
      new NumberProperty<Integer>(2, 1, 8, "Blocks Per Tick");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> tower = new Property<Boolean>(true, "Tower");

  public Scaffold() {
    super("Scaffold", new String[] {"scaffold", "bridge"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Places blocks under you for bridging and towering.");
    offerProperties(blocksPerTick, rotate, autoSwitch, swingHand, tower);
    this.listeners.add(
        new Listener<TickEvent>("scaffold_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Scaffold.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    int blockSlot = PlayerUtil.findInHotbar(Scaffold::isPlaceable);
    if (blockSlot == -1) return;

    BlockPos feet = minecraft.player.blockPosition();
    BlockPos[] targets = {feet.below(), feet.relative(minecraft.player.getDirection()).below()};
    int placed = 0;
    for (BlockPos pos : targets) {
      if (placed >= blocksPerTick.getValue()) break;
      if (!PlayerUtil.isAirOrReplaceable(pos)) continue;
      place(pos, blockSlot);
      placed++;
    }

    // Tower: jump right as the block seats so you keep climbing.
    if (tower.getValue()
        && placed > 0
        && minecraft.player.onGround()
        && minecraft.options.keyJump.isDown()) {
      minecraft.player.jumpFromGround();
    }
  }

  private void place(BlockPos pos, int slot) {
    boolean swapped = PlayerUtil.swapToSlot(slot, autoSwitch.getValue());
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(pos), PlayerUtil.getPitch(pos));
    }
    PlayerUtil.clickNeighbor(pos);
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    PlayerUtil.swapBackIf(swapped);
  }

  private static boolean isPlaceable(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return false;
    return true;
  }
}
