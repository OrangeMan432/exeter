package me.friendly.exeter.module.impl.toggle.misc;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.StopWatch;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/**
 * Port of 3arthh4ck's Replenish: remembers the hotbar and tops stacks back up from the main
 * inventory once they fall to Threshold. One move per tick max, container slots 9-35 are donors,
 * 36-44 are the hotbar.
 */
public class Replenish extends ToggleableModule {

  private final NumberProperty<Integer> threshold =
      new NumberProperty<Integer>(32, 0, 64, "Threshold");
  private final NumberProperty<Integer> delay = new NumberProperty<Integer>(50, 0, 500, "Delay");
  private final Property<Boolean> putBack = new Property<Boolean>(true, "Put Back");
  private final Property<Boolean> replenishInLoot =
      new Property<Boolean>(true, "Replenish In Loot");

  private final List<ItemStack> memory = new ArrayList<>(9);
  private final StopWatch timer = new StopWatch();

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("replenish_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          onTick();
        }
      };

  public Replenish() {
    super("Replenish", new String[] {"replenish", "refill"}, 0x66CCFF, ModuleType.MISCELLANEOUS);
    setDescription("Refills hotbar stacks from the main inventory.");
    offerProperties(threshold, delay, putBack, replenishInLoot);
    threshold.setDescription("Refill hotbar stacks at or below this count.");
    delay.setDescription("Milliseconds between inventory moves.");
    putBack.setDescription("Return leftover items to the donor slot.");
    replenishInLoot.setDescription("Keep refilling while dropped items are nearby.");
    clearMemory();
    this.listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    clearMemory();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    clearMemory();
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  private void onTick() {
    if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) return;
    if (minecraft.player.getAbilities().instabuild) return;
    // In a container: forget the hotbar so closing it doesn't rearrange anything.
    if (minecraft.player.containerMenu != minecraft.player.inventoryMenu) {
      clearMemory();
      return;
    }
    // Same while arranging items by hand: never fight a manual drag.
    if (minecraft.gui.screen() instanceof AbstractContainerScreen) {
      clearMemory();
      return;
    }
    if (!timer.hasPassed(delay.getValue())) return;
    if (!replenishInLoot.getValue() && lootNearby()) return;

    ItemStack carried = minecraft.player.containerMenu.getCarried();
    for (int i = 0; i < 9; i++) {
      ItemStack current = minecraft.player.containerMenu.getSlot(36 + i).getItem();
      ItemStack remembered = memory.get(i);
      boolean needsRefill =
          !current.isEmpty()
              && current.getCount() <= threshold.getValue()
              && current.getCount() < current.getMaxStackSize();
      boolean slotEmptied = current.isEmpty() && !remembered.isEmpty();
      if (!needsRefill && !slotEmptied) {
        memory.set(i, current.copy());
        continue;
      }
      ItemStack want = slotEmptied ? remembered : current;
      // Unstackables (beds, tools, armor) can't be topped up; shuffling them
      // mid-fight only desyncs modules holding their slots, e.g. BedAura.
      if (want.getMaxStackSize() <= 1) {
        memory.set(i, current.copy());
        continue;
      }
      // Cursor first: merge what we are already holding before opening new stacks.
      if (!carried.isEmpty()) {
        if (!needsRefill || !canStack(carried, want)) return;
        click(36 + i);
        DebugLogger.get()
            .log(
                getLabel(),
                DebugLogger.Level.INFO,
                "merged cursor into hotbar slot " + i + " (" + want.getItem().toString() + ")");
        memory.set(i, minecraft.player.containerMenu.getSlot(36 + i).getItem().copy());
        timer.reset();
        return;
      }
      if (!remembered.isEmpty() && !canStack(remembered, want) && !slotEmptied) {
        memory.set(i, current.copy());
        continue;
      }
      int source = findDonor(want, want.getCount());
      if (source == -1) {
        memory.set(i, current.copy());
        continue;
      }
      merge(source, 36 + i);
      DebugLogger.get()
          .log(
              getLabel(),
              DebugLogger.Level.INFO,
              "refilled hotbar slot "
                  + i
                  + " from container slot "
                  + source
                  + " ("
                  + want.getItem().toString()
                  + " x"
                  + want.getCount()
                  + ")");
      memory.set(i, minecraft.player.containerMenu.getSlot(36 + i).getItem().copy());
      timer.reset();
      // One move per tick; delay 0 still yields to the server between clicks.
      return;
    }
  }

  /**
   * Best donor for the wanted stack: biggest count that fits without overfilling, else the biggest
   * stack overall (remainder goes back when Put Back is on). Overfill donors are skipped entirely
   * with Put Back off so nothing strands on the cursor.
   */
  private int findDonor(ItemStack want, int have) {
    int space = want.getMaxStackSize() - have;
    int fitSlot = -1;
    int fitCount = -1;
    int bigSlot = -1;
    int bigCount = -1;
    for (int slot = 9; slot <= 35; slot++) {
      ItemStack donor = minecraft.player.containerMenu.getSlot(slot).getItem();
      if (!canStack(donor, want)) continue;
      int count = donor.getCount();
      if (count <= space && count > fitCount) {
        fitSlot = slot;
        fitCount = count;
      }
      if (count > bigCount) {
        bigSlot = slot;
        bigCount = count;
      }
    }
    if (fitSlot != -1) return fitSlot;
    if (bigSlot != -1 && (putBack.getValue() || bigCount <= space)) return bigSlot;
    return -1;
  }

  /** Pickup-merge: lift the donor, merge into the hotbar slot, return the remainder. */
  private void merge(int sourceSlot, int hotbarSlot) {
    click(sourceSlot);
    click(hotbarSlot);
    if (putBack.getValue() && !minecraft.player.containerMenu.getCarried().isEmpty()) {
      click(sourceSlot);
    }
  }

  private void click(int slot) {
    minecraft.gameMode.handleContainerInput(
        minecraft.player.containerMenu.containerId,
        slot,
        0,
        ContainerInput.PICKUP,
        minecraft.player);
  }

  /** Same item with identical components (count excluded): safe to merge. */
  private static boolean canStack(ItemStack a, ItemStack b) {
    if (a.isEmpty() || b.isEmpty()) return false;
    return a.getItem() == b.getItem() && a.getComponents().equals(b.getComponents());
  }

  private boolean lootNearby() {
    for (ItemEntity item :
        minecraft.level.getEntitiesOfClass(
            ItemEntity.class, minecraft.player.getBoundingBox().inflate(2.0))) {
      if (!item.isRemoved()) return true;
    }
    return false;
  }

  private void clearMemory() {
    memory.clear();
    for (int i = 0; i < 9; i++) {
      memory.add(ItemStack.EMPTY);
    }
  }
}
