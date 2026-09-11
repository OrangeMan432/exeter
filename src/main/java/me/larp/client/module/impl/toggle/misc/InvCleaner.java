package me.larp.client.module.impl.toggle.misc;

import java.util.Set;
import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shoreline-pattern InvCleaner: drops junk items on a timer so loot runs
 * never clog. Fixed junk set, hotbar included on demand.
 */
public class InvCleaner extends ToggleableModule {

  private static final Set<String> JUNK = Set.of(
      "minecraft:rotten_flesh",
      "minecraft:poisonous_potato",
      "minecraft:spider_eye",
      "minecraft:fermented_spider_eye",
      "minecraft:gunpowder",
      "minecraft:string",
      "minecraft:bone",
      "minecraft:arrow",
      "minecraft:bowl",
      "minecraft:glass_bottle",
      "minecraft:leather",
      "minecraft:feather",
      "minecraft:stick",
      "minecraft:snowball",
      "minecraft:egg",
      "minecraft:seeds",
      "minecraft:wheat_seeds",
      "minecraft:beetroot_seeds",
      "minecraft:pumpkin_seeds",
      "minecraft:melon_seeds",
      "minecraft:torchflower_seeds",
      "minecraft:pitcher_pod");

  private final NumberProperty<Double> delay =
      new NumberProperty<Double>(0.2, 0.0, 2.0, "Delay");
  private final Property<Boolean> hotbar =
      new Property<Boolean>(true, "Hotbar");

  private long lastDrop;

  public InvCleaner() {
    super("InvCleaner", new String[] {"invcleaner", "inv-cleaner"}, 0x00FFFF,
        ModuleType.MISCELLANEOUS);
    setDescription("Drops junk items automatically.");
    offerProperties(delay, hotbar);
    this.listeners.add(
        new Listener<TickEvent>("invcleaner_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            InvCleaner.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    lastDrop = 0;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (minecraft.player.containerMenu != null
        && minecraft.player.containerMenu != minecraft.player.inventoryMenu) return;
    if (System.currentTimeMillis() - lastDrop < delay.getValue() * 1000L) return;

    int end = hotbar.getValue() ? 45 : 36;
    for (int i = 9; i < end; i++) {
      ItemStack stack = minecraft.player.containerMenu.getSlot(i).getItem();
      if (stack.isEmpty()) continue;
      // Never drop equipped armor, tools, or food.
      if (stack.get(net.minecraft.core.component.DataComponents.FOOD) != null) continue;
      if (stack.isEnchanted()) continue;
      String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
      if (!JUNK.contains(id)) continue;
      minecraft.gameMode.handleContainerInput(
          minecraft.player.containerMenu.containerId, i, 1, ContainerInput.THROW,
          minecraft.player);
      lastDrop = System.currentTimeMillis();
      return;
    }
  }
}
