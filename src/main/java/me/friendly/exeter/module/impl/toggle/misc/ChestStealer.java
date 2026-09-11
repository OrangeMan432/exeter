package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ShulkerBoxMenu;

/** Empties chests and shulkers into your inventory on a delay. */
public class ChestStealer extends ToggleableModule {

  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(2, 0, 20, "Delay");
  private final Property<Boolean> shulkers =
      new Property<Boolean>(true, "Shulkers");
  private final Property<Boolean> autoClose =
      new Property<Boolean>(true, "Auto Close");

  private int tickCounter;

  public ChestStealer() {
    super("ChestStealer", new String[] {"cheststealer", "stealer"}, 0x00FFFF,
        ModuleType.MISCELLANEOUS);
    setDescription("Loots containers automatically.");
    offerProperties(delay, shulkers, autoClose);
    this.listeners.add(
        new Listener<TickEvent>("cheststealer_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            ChestStealer.this.onTick();
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

    int size = containerSize();
    if (size == -1) return;
    if (tickCounter++ < delay.getValue()) return;

    for (int i = 0; i < size; i++) {
      if (minecraft.player.containerMenu.getSlot(i).getItem().isEmpty()) continue;
      minecraft.gameMode.handleContainerInput(
          minecraft.player.containerMenu.containerId, i, 0, ContainerInput.QUICK_MOVE,
          minecraft.player);
      tickCounter = 0;
      return;
    }

    if (autoClose.getValue()) {
      minecraft.player.closeContainer();
    }
  }

  private int containerSize() {
    if (minecraft.player.containerMenu instanceof ChestMenu chest) {
      return chest.getContainer().getContainerSize();
    }
    if (shulkers.getValue() && minecraft.player.containerMenu instanceof ShulkerBoxMenu) {
      return 27;
    }
    return -1;
  }
}
