package me.larp.client.module.impl.toggle.combat;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

/**
 * Shoreline-pattern offhand manager: absorption-aware health, fall-lethal fast-path, gapple while
 * holding use on a sword, live totem count tag.
 */
public class AutoTotem extends ToggleableModule {

  private final NumberProperty<Double> minHealth =
      new NumberProperty<Double>(14.0, 0.0, 36.0, "Min Health");
  private final Property<Boolean> gappleSwap = new Property<Boolean>(true, "Gapple Swap");
  private final Property<Boolean> fallLethal = new Property<Boolean>(true, "Fall Lethal");

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("autototem_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          onTick();
        }
      };

  public AutoTotem() {
    super("AutoTotem", new String[] {"autototem", "auto-totem"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Manages your offhand: totems, gapples, fall-lethal swaps.");
    offerProperties(minHealth, gappleSwap, fallLethal);
    this.listeners.add(tickListener);
  }

  private void onTick() {
    if (minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.containerMenu != null
        && minecraft.player.containerMenu != minecraft.player.inventoryMenu) return;
    // Never click while holding an item on the cursor (would dupe-drop it).
    if (!minecraft.player.containerMenu.getCarried().isEmpty()) return;

    setTag("AutoTotem [" + countTotems() + "]");

    boolean wantTotem = wantTotem();
    boolean holdingTotem = minecraft.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING;
    boolean holdingGapple =
        minecraft.player.getOffhandItem().getItem() == Items.GOLDEN_APPLE
            || minecraft.player.getOffhandItem().getItem() == Items.ENCHANTED_GOLDEN_APPLE;

    if (wantTotem && holdingTotem) return;
    if (!wantTotem && (holdingGapple || holdingTotem)) {
      // Holding something usable: only swap gapple in, never strip a totem for fun.
      if (holdingTotem || !gappleSwap.getValue()) return;
    }

    net.minecraft.world.item.Item target = wantTotem ? Items.TOTEM_OF_UNDYING : Items.GOLDEN_APPLE;
    if (minecraft.player.getOffhandItem().getItem() == target) return;

    for (int i = 9; i < 45; i++) {
      if (i == minecraft.player.getInventory().getSelectedSlot() + 36) continue;
      if (minecraft.player.containerMenu.getSlot(i).getItem().getItem() == target) {
        int containerId = minecraft.player.containerMenu.containerId;
        minecraft.gameMode.handleContainerInput(
            containerId, i, 0, ContainerInput.PICKUP, minecraft.player);
        minecraft.gameMode.handleContainerInput(
            containerId, 45, 0, ContainerInput.PICKUP, minecraft.player);
        // Put back whatever we picked up from the offhand.
        if (!minecraft.player.containerMenu.getCarried().isEmpty()
            && minecraft.player.containerMenu.getCarried().getItem() != target) {
          minecraft.gameMode.handleContainerInput(
              containerId, i, 0, ContainerInput.PICKUP, minecraft.player);
        }
        return;
      }
    }
    // No gapple found: fall back to totem so the offhand is never empty.
    if (!wantTotem && target == Items.GOLDEN_APPLE) {
      for (int i = 9; i < 45; i++) {
        if (minecraft.player.containerMenu.getSlot(i).getItem().getItem()
            == Items.TOTEM_OF_UNDYING) {
          int containerId = minecraft.player.containerMenu.containerId;
          minecraft.gameMode.handleContainerInput(
              containerId, i, 0, ContainerInput.PICKUP, minecraft.player);
          minecraft.gameMode.handleContainerInput(
              containerId, 45, 0, ContainerInput.PICKUP, minecraft.player);
          return;
        }
      }
    }
  }

  /** Shoreline-pattern: absorption counts, fall damage can be lethal, use-key wants gapple. */
  private boolean wantTotem() {
    float effective = minecraft.player.getHealth() + minecraft.player.getAbsorptionAmount();
    if (effective <= minHealth.getValue().floatValue()) return true;
    if (fallLethal.getValue()) {
      // Rough vanilla fall math: (distance - 3) / 2 hearts, boots ignored (safe side).
      float fallHearts = (float) ((minecraft.player.fallDistance - 3.0) / 2.0);
      if (fallHearts + 0.5f > effective) return true;
    }
    // Holding right-click on a sword: Shoreline equips gapple instead.
    if (gappleSwap.getValue()
        && minecraft.player.isUsingItem()
        && isSwordHeld()
        && effective > minHealth.getValue().floatValue()) {
      return false;
    }
    return true;
  }

  private boolean isSwordHeld() {
    var held = minecraft.player.getMainHandItem();
    if (held.isEmpty()) return false;
    return net.minecraft.core.registries.BuiltInRegistries.ITEM
        .getKey(held.getItem())
        .getPath()
        .endsWith("_sword");
  }

  private int countTotems() {
    if (minecraft.player == null) return 0;
    int count = 0;
    for (int i = 0; i < 45; i++) {
      if (minecraft.player.containerMenu.getSlot(i).getItem().getItem() == Items.TOTEM_OF_UNDYING) {
        count += minecraft.player.containerMenu.getSlot(i).getItem().getCount();
      }
    }
    return count;
  }
}
