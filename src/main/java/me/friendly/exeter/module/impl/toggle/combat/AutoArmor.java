package me.friendly.exeter.module.impl.toggle.combat;

import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class AutoArmor extends ToggleableModule {

  // Vanilla defense values per slot type (0 helmet, 1 chest, 2 legs, 3 boots)
  // and material tier (0 leather, 1 chain, 2 iron, 3 diamond, 4 gold).
  private static final int[][] DEFENSE = {
    {1, 2, 2, 3, 2},
    {3, 5, 6, 8, 5},
    {2, 4, 5, 6, 3},
    {1, 1, 2, 3, 1}
  };

  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(1, 1, 10, "Delay", "delay");
  private final Property<Boolean> armorSaver = new Property<Boolean>(false, "Armor Saver", "saver");
  private final NumberProperty<Integer> depletion =
      new NumberProperty<Integer>(20, 0, 99, "Depletion", "depletion");

  private int tickCounter;

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("autoarmor_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          AutoArmor.this.onTick();
        }
      };

  public AutoArmor() {
    super("AutoArmor", new String[] {"autoarmor", "auto-armor"}, 0xFF5500, ModuleType.COMBAT);
    setDescription("Automatically equips the best armor from your inventory.");
    offerProperties(delay, armorSaver, depletion);
    this.listeners.add(tickListener);
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    tickCounter = 0;
  }

  private static boolean isArmor(ItemStack stack) {
    return stack != null && stack.count > 0 && stack.itemId >= 298 && stack.itemId <= 317;
  }

  private static int armorType(ItemStack stack) {
    return (stack.itemId - 298) % 4;
  }

  private static int score(ItemStack stack) {
    int type = armorType(stack);
    int tier = (stack.itemId - 298) / 4;
    int defense = DEFENSE[type][tier];
    int remaining = 100;
    if (stack.isDamageable() && stack.getMaxDamage() > 0) {
      remaining = (stack.getMaxDamage() - stack.getDamage()) * 100 / stack.getMaxDamage();
    }
    return defense * 100 + remaining;
  }

  private void onTick() {
    if (minecraft() == null || minecraft().player == null || minecraft().world == null) {
      return;
    }
    PlayerEntity player = minecraft().player;
    if (player.dead) {
      return;
    }
    if (player.container != player.playerContainer) {
      // A chest/crafting GUI is open; only manage the closed player container.
      return;
    }
    tickCounter++;
    if (tickCounter % Math.max(1, delay.getValue().intValue()) != 0) {
      return;
    }
    // armor[3] helmet, armor[2] chest, armor[1] legs, armor[0] boots.
    for (int type = 0; type < 4; type++) {
      int armorIndex = 3 - type;
      ItemStack current =
          armorIndex < player.inventory.armor.length ? player.inventory.armor[armorIndex] : null;
      if (armorSaver.getValue().booleanValue()
          && isArmor(current)
          && current.isDamageable()
          && current.getMaxDamage() > 0) {
        int remaining =
            (current.getMaxDamage() - current.getDamage()) * 100 / current.getMaxDamage();
        if (remaining <= depletion.getValue().intValue()) {
          unequip(player, current);
          continue;
        }
      }
      int currentScore = isArmor(current) ? score(current) : -1;
      Slot best = null;
      int bestScore = currentScore;
      List slots = player.playerContainer.slots;
      for (int i = 0; i < slots.size(); i++) {
        Slot slot = (Slot) slots.get(i);
        ItemStack stack = slot.getStack();
        if (!isArmor(stack) || armorType(stack) != type) continue;
        int s = score(stack);
        if (s > bestScore) {
          bestScore = s;
          best = slot;
        }
      }
      if (best != null) {
        // Vanilla shift-click equips (and swaps) armor automatically.
        minecraft().interactionManager.clickSlot(
            player.playerContainer.syncId, best.id, 0, true, player);
      }
    }
  }

  private void unequip(PlayerEntity player, ItemStack current) {
    List slots = player.playerContainer.slots;
    for (int i = 0; i < slots.size(); i++) {
      Slot slot = (Slot) slots.get(i);
      if (slot.getStack() == current) {
        minecraft().interactionManager.clickSlot(
            player.playerContainer.syncId, slot.id, 0, true, player);
        return;
      }
    }
  }
}
