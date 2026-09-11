package me.larp.client.module.impl.toggle.misc;

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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/**
 * Techale-pattern 5b5t shulker dupe (via ToxicAven's Lambda plugin): throw the
 * shulker, craft a button on the table you stand on, place the stacked result,
 * mine it with a pickaxe. Stand on a crafting table to start.
 */
public class ShulkerDupe extends ToggleableModule {

  private enum Phase {
    THROW,
    WAIT_OPEN,
    CRAFT,
    WAIT_STACK,
    MINE,
    DONE
  }

  private final NumberProperty<Integer> craftTicks =
      new NumberProperty<Integer>(60, 0, 200, "Craft Ticks");
  private final NumberProperty<Integer> stackTimeout =
      new NumberProperty<Integer>(120, 20, 400, "Stack Timeout");
  private final NumberProperty<Integer> mineTimeout =
      new NumberProperty<Integer>(200, 20, 600, "Mine Timeout");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");

  private Phase phase = Phase.THROW;
  private int phaseTick;
  private int shulkerSlot = -1;
  private BlockPos shulkerPos;

  public ShulkerDupe() {
    super("ShulkerDupe", new String[] {"shulkerdupe", "shulker-dupe"}, 0x00FFFF,
        ModuleType.MISCELLANEOUS);
    setDescription("5b5t shulker dupe: table, shulker, pickaxe, planks.");
    offerProperties(craftTicks, stackTimeout, mineTimeout, rotate);
    this.listeners.add(
        new Listener<TickEvent>("shulkerdupe_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            ShulkerDupe.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    phaseTick = 0;
    if (minecraft.player == null || minecraft.level == null) {
      setRunning(false);
      return;
    }
    // Must stand on a crafting table.
    BlockPos below = minecraft.player.blockPosition().below();
    if (minecraft.level.getBlockState(below).getBlock() != Blocks.CRAFTING_TABLE) {
      setRunning(false);
      return;
    }
    shulkerSlot = findShulkerHotbar();
    if (shulkerSlot == -1 || findPickaxe() == -1 || findPlanks() == -1) {
      setRunning(false);
      return;
    }
    shulkerPos = findShulkerPos();
    if (shulkerPos == null) {
      setRunning(false);
      return;
    }
    phase = Phase.THROW;
  }

  private void onTick() {
    if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) {
      setRunning(false);
      return;
    }
    phaseTick++;

    switch (phase) {
      case THROW -> {
        if (phaseTick < 3) return;
        // Drop the shulker, then open the table below.
        minecraft.player.containerMenu.clicked(
            shulkerSlot + 36, 1, ContainerInput.THROW, minecraft.player);
        openTable();
        advance(Phase.WAIT_OPEN);
      }
      case WAIT_OPEN -> {
        if (!(minecraft.player.containerMenu instanceof CraftingMenu)) {
          if (phaseTick > 40) {
            setRunning(false);
          }
          return;
        }
        advance(Phase.CRAFT);
      }
      case CRAFT -> {
        if (phaseTick < craftTicks.getValue()) return;
        int wood = findPlanks();
        if (wood == -1) {
          setRunning(false);
          return;
        }
        int woodContainer = wood < 9 ? wood + 36 : wood;
        // Right-click pickup half the planks, drop one on grid slot 1.
        minecraft.gameMode.handleContainerInput(
            minecraft.player.containerMenu.containerId,
            woodContainer,
            1,
            ContainerInput.PICKUP,
            minecraft.player);
        minecraft.gameMode.handleContainerInput(
            minecraft.player.containerMenu.containerId, 1, 0, ContainerInput.PICKUP,
            minecraft.player);
        advance(Phase.WAIT_STACK);
      }
      case WAIT_STACK -> {
        if (findStackedShulker() != -1) {
          minecraft.player.closeContainer();
          placeStacked();
          advance(Phase.MINE);
        } else if (phaseTick > stackTimeout.getValue()) {
          minecraft.player.closeContainer();
          setRunning(false);
        }
      }
      case MINE -> {
        if (shulkerPos == null) {
          setRunning(false);
          return;
        }
        if (!(minecraft.level.getBlockState(shulkerPos).getBlock() instanceof ShulkerBoxBlock)) {
          advance(Phase.DONE);
          return;
        }
        if (phaseTick > mineTimeout.getValue()) {
          setRunning(false);
          return;
        }
        int pick = findPickaxe();
        if (pick == -1) {
          setRunning(false);
          return;
        }
        boolean swapped = PlayerUtil.swapToSlot(pick, true);
        PlayerUtil.breakBlock(shulkerPos, Direction.UP);
        PlayerUtil.swingHand();
        PlayerUtil.swapBackIf(swapped);
      }
      case DONE -> setRunning(false);
    }
  }

  private void advance(Phase next) {
    phase = next;
    phaseTick = 0;
  }

  private void openTable() {
    BlockPos table = minecraft.player.blockPosition().below();
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(yaw, 90f);
    }
    PlayerUtil.useItemOn(table, Direction.UP);
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
  }

  private void placeStacked() {
    int slot = findStackedShulker();
    if (slot == -1) return;
    boolean swapped = PlayerUtil.swapToSlot(slot, true);
    minecraft.player.setShiftKeyDown(true);
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(shulkerPos), PlayerUtil.getPitch(shulkerPos));
    }
    PlayerUtil.clickNeighbor(shulkerPos);
    PlayerUtil.swingHand();
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    minecraft.player.setShiftKeyDown(false);
    PlayerUtil.swapBackIf(swapped);
  }

  private int findShulkerHotbar() {
    for (int i = 0; i < 9; i++) {
      if (isShulker(minecraft.player.getInventory().getItem(i))) return i;
    }
    return -1;
  }

  private int findStackedShulker() {
    for (int i = 0; i < 9; i++) {
      ItemStack stack = minecraft.player.getInventory().getItem(i);
      if (isShulker(stack) && stack.getCount() > 1) return i;
    }
    return -1;
  }

  private int findPickaxe() {
    return PlayerUtil.findInHotbar(
        s -> {
          if (s.isEmpty()) return false;
          String path =
              net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
          return path.endsWith("_pickaxe");
        });
  }

  private int findPlanks() {
    for (int i = 0; i < 36; i++) {
      ItemStack stack = minecraft.player.getInventory().getItem(i);
      if (!stack.isEmpty()
          && stack.getItem() instanceof BlockItem item
          && item.getBlock().defaultBlockState().is(BlockTags.PLANKS)) {
        return i;
      }
    }
    return -1;
  }

  private BlockPos findShulkerPos() {
    BlockPos feet = minecraft.player.blockPosition();
    for (Direction dir : Direction.Plane.HORIZONTAL) {
      BlockPos pos = feet.relative(dir);
      if (PlayerUtil.isAirOrReplaceable(pos)) return pos;
    }
    return null;
  }

  private static boolean isShulker(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() instanceof ShulkerBoxBlock;
  }
}
