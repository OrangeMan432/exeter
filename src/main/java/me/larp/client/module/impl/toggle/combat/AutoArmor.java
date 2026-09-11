package me.larp.client.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Shoreline-pattern AutoArmor for 26.2 (no ArmorItem class here): scores EQUIPPABLE pieces by tier
 * + blast priority, gates on durability, skips binding curses and prized elytras. Equips one piece
 * per tick via shift-click.
 */
public class AutoArmor extends ToggleableModule {

  public enum Priority {
    BLAST,
    PROTECTION
  }

  private final EnumProperty<Priority> priority =
      new EnumProperty<Priority>(Priority.BLAST, "Priority");
  private final NumberProperty<Double> minDurability =
      new NumberProperty<Double>(0.0, 0.0, 0.2, "Min Durability");
  private final Property<Boolean> elytraPriority = new Property<Boolean>(true, "Elytra Priority");
  private final Property<Boolean> noBinding = new Property<Boolean>(true, "No Binding");

  public AutoArmor() {
    super("AutoArmor", new String[] {"autoarmor", "auto-armor"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Replaces broken armor with the best blast-prot pieces.");
    offerProperties(priority, minDurability, elytraPriority, noBinding);
    this.listeners.add(
        new Listener<TickEvent>("autoarmor_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoArmor.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (minecraft.player.containerMenu != null
        && minecraft.player.containerMenu != minecraft.player.inventoryMenu) return;

    Holder<Enchantment> blast = resolve(Enchantments.BLAST_PROTECTION);
    Holder<Enchantment> prot = resolve(Enchantments.PROTECTION);
    Holder<Enchantment> binding = resolve(Enchantments.BINDING_CURSE);

    for (EquipmentSlot slot :
        new EquipmentSlot[] {
          EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
      ItemStack worn = minecraft.player.getItemBySlot(slot);
      if (elytraPriority.getValue()
          && slot == EquipmentSlot.CHEST
          && worn.getItem() == Items.ELYTRA) {
        continue;
      }
      if (!worn.isEmpty() && durability(worn) >= minDurability.getValue()) {
        continue;
      }
      int best = findBest(slot, blast, prot, binding);
      if (best == -1) continue;
      // Shift-click moves it straight into its armor slot.
      minecraft.gameMode.handleContainerInput(
          minecraft.player.containerMenu.containerId,
          best,
          0,
          ContainerInput.QUICK_MOVE,
          minecraft.player);
      return;
    }
  }

  private int findBest(
      EquipmentSlot slot,
      Holder<Enchantment> blast,
      Holder<Enchantment> prot,
      Holder<Enchantment> binding) {
    List<int[]> candidates = new ArrayList<>();
    for (int i = 9; i < 45; i++) {
      ItemStack stack = minecraft.player.containerMenu.getSlot(i).getItem();
      if (stack.isEmpty()) continue;
      var equippable = stack.get(DataComponents.EQUIPPABLE);
      if (equippable == null || equippable.slot() != slot) continue;
      if (noBinding.getValue() && binding != null && level(stack, binding) > 0) continue;
      if (durability(stack) < minDurability.getValue()) continue;
      candidates.add(new int[] {i, score(stack, blast, prot)});
    }
    return candidates.stream().max(Comparator.comparingInt(a -> a[1])).map(a -> a[0]).orElse(-1);
  }

  private int score(ItemStack stack, Holder<Enchantment> blast, Holder<Enchantment> prot) {
    // Within one armor slot, max damage orders tiers correctly.
    int score = stack.getMaxDamage();
    if (blast != null) {
      score += level(stack, blast) * 100;
    }
    if (prot != null) {
      score += level(stack, prot) * (priority.getValue() == Priority.PROTECTION ? 100 : 25);
    }
    if (stack.isEnchanted()) {
      score += 10;
    }
    return score;
  }

  private int level(ItemStack stack, Holder<Enchantment> enchant) {
    try {
      return stack.getEnchantments().getLevel(enchant);
    } catch (Exception e) {
      return 0;
    }
  }

  private double durability(ItemStack stack) {
    if (stack.getMaxDamage() <= 0) return 1.0;
    return (stack.getMaxDamage() - stack.getDamageValue()) / (double) stack.getMaxDamage();
  }

  private Holder<Enchantment> resolve(net.minecraft.resources.ResourceKey<Enchantment> key) {
    try {
      return minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    } catch (Exception e) {
      return null;
    }
  }
}
