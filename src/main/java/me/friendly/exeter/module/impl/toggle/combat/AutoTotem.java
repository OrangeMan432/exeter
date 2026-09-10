package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

public class AutoTotem extends ToggleableModule {

  private final NumberProperty<Double> minHealth =
      new NumberProperty<Double>(12.0, 0.0, 36.0, "Min Health");
  private final Property<Boolean> gappleSwap =
      new Property<Boolean>(true, "Gapple Swap");

  private int tickDelay;

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
    setDescription("Equips totems of undying into your offhand automatically.");
    offerProperties(minHealth, gappleSwap);
    this.listeners.add(tickListener);
  }

  private void onTick() {
    if (minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.containerMenu != null
        && minecraft.player.containerMenu != minecraft.player.inventoryMenu) return;
    // Never click while holding an item on the cursor (would dupe-drop it).
    if (!minecraft.player.containerMenu.getCarried().isEmpty()) return;

    boolean holdingTotem =
        minecraft.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING;
    setTag("AutoTotem [" + countTotems() + "]");
    if (holdingTotem) {
      tickDelay = 0;
      return;
    }

    boolean lethal =
        minecraft.player.getHealth() <= minHealth.getValue().floatValue();
    // Lethal range bypasses the tick delay so the totem lands before the next hit.
    if (!lethal && ++tickDelay < 1) return;
    tickDelay = 0;

    for (int i = 9; i < 45; i++) {
      if (i == minecraft.player.getInventory().getSelectedSlot() + 36) continue;
      if (minecraft.player.containerMenu.getSlot(i).getItem().getItem() == Items.TOTEM_OF_UNDYING) {
        int containerId = minecraft.player.containerMenu.containerId;
        minecraft.gameMode.handleContainerInput(
            containerId, i, 0, ContainerInput.PICKUP, minecraft.player);
        minecraft.gameMode.handleContainerInput(
            containerId, 45, 0, ContainerInput.PICKUP, minecraft.player);
        // If we picked up a non-totem with the second click (offhand had gapple),
        // put it back into the origin slot instead of leaving it on the cursor.
        if (gappleSwap.getValue()
            && !minecraft.player.containerMenu.getCarried().isEmpty()
            && minecraft.player.containerMenu.getCarried().getItem() != Items.TOTEM_OF_UNDYING) {
          minecraft.gameMode.handleContainerInput(
              containerId, i, 0, ContainerInput.PICKUP, minecraft.player);
        }
        return;
      }
    }
  }

  private int countTotems() {
    if (minecraft.player == null) return 0;
    int count = 0;
    for (int i = 0; i < 45; i++) {
      if (minecraft.player.containerMenu.getSlot(i).getItem().getItem()
          == Items.TOTEM_OF_UNDYING) {
        count += minecraft.player.containerMenu.getSlot(i).getItem().getCount();
      }
    }
    return count;
  }
}
