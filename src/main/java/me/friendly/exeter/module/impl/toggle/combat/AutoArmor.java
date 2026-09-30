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

  // Beta has no shift-click quick-move: equip via pickup/place clicks.
  // Player container layout (verified): 0 craft result, 1-4 craft grid,
  // 5-8 armor (5 helmet .. 8 boots), 9-35 main, 36-44 hotbar.
  private static final int ARMOR_SLOT_BASE = 5;
  private static final int INV_SLOT_START = 9;
  private static final int INV_SLOT_END = 44;

  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(2, 1, 20, "Delay", "delay");
  private final Property<Boolean> armorSaver = new Property<Boolean>(false, "Armor Saver", "saver");
  private final NumberProperty<Integer> depletion =
      new NumberProperty<Integer>(20, 0, 99, "Depletion", "depletion");

  private int tickCounter;
  private int pendingSource = -1;
  private int pendingArmorSlot = -1;
  private int pendingReturnSlot = -1;
  private int pendingType = -1;

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
    clearPending();
  }

  private void clearPending() {
    pendingSource = -1;
    pendingArmorSlot = -1;
    pendingReturnSlot = -1;
    pendingType = -1;
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

  private Slot containerSlot(PlayerEntity player, int id) {
    List slots = player.playerContainer.slots;
    if (id < 0 || id >= slots.size()) return null;
    return (Slot) slots.get(id);
  }

  private void click(PlayerEntity player, int slotId) {
    minecraft().interactionManager.clickSlot(
        player.playerContainer.syncId, slotId, 0, false, player);
  }

  private void onTick() {
    if (minecraft() == null || minecraft().player == null || minecraft().world == null) {
      return;
    }
    PlayerEntity player = minecraft().player;
    if (player.dead) {
      clearPending();
      return;
    }
    if (player.container != player.playerContainer) {
      // A chest/crafting GUI is open; only manage the closed player container.
      clearPending();
      return;
    }
    tickCounter++;
    if (tickCounter % Math.max(1, delay.getValue().intValue()) != 0) {
      return;
    }
    if (pendingSource != -1) {
      finishPending(player);
      return;
    }
    // armor[3] helmet, armor[2] chest, armor[1] legs, armor[0] boots.
    for (int type = 0; type < 4; type++) {
      int armorSlot = ARMOR_SLOT_BASE + type;
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
          if (startUnequip(player, armorSlot)) return;
          continue;
        }
      }
      int currentScore = isArmor(current) ? score(current) : -1;
      int bestSlot = -1;
      int bestScore = currentScore;
      for (int slotId = INV_SLOT_START; slotId <= INV_SLOT_END; slotId++) {
        Slot slot = containerSlot(player, slotId);
        if (slot == null) continue;
        ItemStack stack = slot.getStack();
        if (!isArmor(stack) || armorType(stack) != type) continue;
        int s = score(stack);
        if (s > bestScore) {
          bestScore = s;
          bestSlot = slotId;
        }
      }
      if (bestSlot != -1) {
        // Step 1: pick up the better piece. Step 2 (next ticks): place into
        // the armor slot, then return any swapped-out piece to the source.
        pendingSource = bestSlot;
        pendingArmorSlot = armorSlot;
        pendingType = type;
        Slot armor = containerSlot(player, armorSlot);
        pendingReturnSlot = (armor != null && isArmor(armor.getStack())) ? bestSlot : -1;
        click(player, bestSlot);
        return;
      }
    }
  }

  private void finishPending(PlayerEntity player) {
    if (pendingArmorSlot != -1) {
      click(player, pendingArmorSlot);
      pendingArmorSlot = -1;
      if (pendingReturnSlot == -1) {
        clearPending();
      }
      return;
    }
    if (pendingReturnSlot != -1) {
      click(player, pendingReturnSlot);
      clearPending();
    }
  }

  private boolean startUnequip(PlayerEntity player, int armorSlot) {
    for (int slotId = INV_SLOT_START; slotId <= INV_SLOT_END; slotId++) {
      Slot slot = containerSlot(player, slotId);
      if (slot == null) continue;
      ItemStack stack = slot.getStack();
      if (stack == null || stack.count > 0) continue;
      pendingSource = armorSlot;
      pendingArmorSlot = slotId;
      pendingReturnSlot = -1;
      pendingType = -1;
      click(player, armorSlot);
      return true;
    }
    return false;
  }
}
