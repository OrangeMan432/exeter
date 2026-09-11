package me.larp.client.module.impl.toggle.combat;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Lemon-pattern AutoSelfFill: fills the hole you stand in with your choice
 * of block so nobody drops in or crystals you out of it.
 */
public class AutoSelfFill extends ToggleableModule {

  public enum FillBlock {
    OBSIDIAN,
    ECHEST,
    WEB
  }

  private final EnumProperty<FillBlock> block =
      new EnumProperty<FillBlock>(FillBlock.OBSIDIAN, "Block");
  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(2, 0, 20, "Delay");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> autoDisable =
      new Property<Boolean>(true, "Auto Disable");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  private int tickCounter;
  private int lastFillTick = -100;

  public AutoSelfFill() {
    super("AutoSelfFill", new String[] {"autoselffill", "self-fill"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Seals yourself inside your hole.");
    offerProperties(block, delay, rotate, autoSwitch, autoDisable, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("autoselffill_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoSelfFill.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
    lastFillTick = -100;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    tickCounter++;
    if (tickCounter - lastFillTick < delay.getValue()) return;

    BlockPos feet = minecraft.player.blockPosition();
    if (!PlayerUtil.isAirOrReplaceable(feet)) {
      if (autoDisable.getValue()) {
        setRunning(false);
      }
      return;
    }

    Block want =
        switch (block.getValue()) {
          case OBSIDIAN -> Blocks.OBSIDIAN;
          case ECHEST -> Blocks.ENDER_CHEST;
          case WEB -> Blocks.COBWEB;
        };
    int slot = PlayerUtil.findInHotbar(s -> isBlock(s, want));
    if (slot == -1) return;
    if (!PlayerUtil.inRange(feet, 5.0)) return;

    boolean swapped = PlayerUtil.swapToSlot(slot, autoSwitch.getValue());
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(feet), PlayerUtil.getPitch(feet));
    }
    PlayerUtil.clickNeighbor(feet);
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    PlayerUtil.swapBackIf(swapped);
    lastFillTick = tickCounter;
  }

  private static boolean isBlock(ItemStack stack, Block block) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == block;
  }
}
