package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/** Meteor-pattern Replenish: refills low hotbar stacks from the main inventory. */
public class Replenish extends ToggleableModule {

  private final NumberProperty<Integer> threshold =
      new NumberProperty<Integer>(8, 1, 64, "Threshold");
  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(2, 0, 20, "Delay");

  private int tickCounter;

  public Replenish() {
    super("Replenish", new String[] {"replenish", "refill"}, 0x00FFFF, ModuleType.MISCELLANEOUS);
    setDescription("Refills low hotbar stacks from your inventory.");
    offerProperties(threshold, delay);
    this.listeners.add(
        new Listener<TickEvent>("replenish_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Replenish.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (minecraft.player.containerMenu != null
        && minecraft.player.containerMenu != minecraft.player.inventoryMenu) return;
    if (tickCounter++ < delay.getValue()) return;

    for (int hotbar = 0; hotbar < 9; hotbar++) {
      ItemStack held = minecraft.player.getInventory().getItem(hotbar);
      if (held.isEmpty() || !held.isStackable()) continue;
      if (held.getCount() > threshold.getValue()) continue;
      int source = findMatching(held, hotbar);
      if (source == -1) continue;
      // Shift-click merges the inventory stack into the hotbar stack.
      minecraft.gameMode.handleContainerInput(
          minecraft.player.containerMenu.containerId, source, 0, ContainerInput.QUICK_MOVE,
          minecraft.player);
      tickCounter = 0;
      return;
    }
  }

  private int findMatching(ItemStack held, int skipHotbar) {
    for (int i = 9; i < 36; i++) {
      ItemStack stack = minecraft.player.containerMenu.getSlot(i).getItem();
      if (stack.isEmpty()) continue;
      if (ItemStack.isSameItemSameComponents(held, stack)) {
        return i;
      }
    }
    return -1;
  }
}
