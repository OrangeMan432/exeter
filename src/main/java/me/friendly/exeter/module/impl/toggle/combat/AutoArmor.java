package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.StopWatch;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;

public class AutoArmor extends ToggleableModule {

  private final NumberProperty<Integer> delay = new NumberProperty<>(1, 1, 10, "Delay");
  private final Property<Boolean> strict = new Property<>(false, "Strict");
  private final Property<Boolean> stackArmor = new Property<>(false, "Stack Armor");
  private final NumberProperty<Integer> swapSlot = new NumberProperty<>(1, 1, 9, "Swap Slot");
  private final Property<Boolean> packetSwitch = new Property<>(true, "Packet Switch");
  private final Property<Boolean> armorSaver = new Property<>(false, "Armor Saver");
  private final NumberProperty<Integer> depletion = new NumberProperty<>(20, 0, 99, "Depletion");
  private final Property<Boolean> elytraSaver = new Property<>(true, "Elytra Saver");
  private final NumberProperty<Integer> elytraDepletion =
      new NumberProperty<>(5, 0, 99, "Elytra Depletion");
  private final Property<Boolean> pauseWhenSafe = new Property<>(false, "Pause When Safe");
  private final Property<Boolean> allowMend = new Property<>(false, "Allow Mend");
  private final NumberProperty<Integer> repairTo = new NumberProperty<>(80, 0, 100, "Repair To");

  private final StopWatch timer = new StopWatch();
  private final StopWatch rightClickTimer = new StopWatch();
  private boolean sleep;

  private static final Minecraft mc = Minecraft.getInstance();

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("autoarmor_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          onTick();
        }
      };

  public AutoArmor() {
    super("AutoArmor", new String[] {"autoarmor", "auto-armor"}, 0xFF5500, ModuleType.COMBAT);
    setDescription("Automatically equips the best armor from your inventory.");
    offerProperties(
        delay,
        strict,
        stackArmor,
        swapSlot,
        packetSwitch,
        armorSaver,
        depletion,
        elytraSaver,
        elytraDepletion,
        pauseWhenSafe,
        allowMend,
        repairTo);
    listeners.add(tickListener);
  }

  private void onTick() {
    if (mc.player == null || mc.level == null || mc.gameMode == null) return;
    if (mc.player.isDeadOrDying()) return;
    if (mc.player.containerMenu != mc.player.inventoryMenu) return;

    if (mc.player.tickCount % delay.getValue() != 0) return;

    if (strict.getValue()
        && (mc.player.xOld != mc.player.getX() || mc.player.zOld != mc.player.getZ())) {
      return;
    }

    if (pauseWhenSafe.getValue() && !hasNearbyEnemies()) return;

    if (allowMend.getValue() && !rightClickTimer.hasPassed(500)) {
      for (int i = 0; i < 4; i++) {
        EquipmentSlot slot = slotFromIndex(i);
        ItemStack armor = mc.player.getItemBySlot(slot);
        if (armor.isEmpty()) continue;
        if (hasEnchantment(armor, Enchantments.MENDING)) {
          int durabilityPercent = getDurabilityPercent(armor);
          if (durabilityPercent < 100 && durabilityPercent > 0) {
            if (!hasEmptyInventorySlot()) return;
            quickMoveArmor(8 - i);
            return;
          }
        }
      }
    }

    if (sleep) {
      sleep = false;
      return;
    }

    List<ArmorCandidate> candidates = findArmorCandidates();

    EquipmentSlot[] slots = {
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    int[] containerSlots = {5, 6, 7, 8};

    for (int s = 0; s < 4; s++) {
      EquipmentSlot equipSlot = slots[s];
      int containerSlot = containerSlots[s];

      // Leave a worn elytra alone while MaceDive is running, or the two modules swap
      // armor back and forth every tick and takeoff becomes impossible.
      if (equipSlot == EquipmentSlot.CHEST
          && mc.player.getItemBySlot(equipSlot).is(Items.ELYTRA)
          && maceDiveRunning()) {
        continue;
      }

      // Pull a worn elytra before it breaks so it never needs phantom-membrane
      // repair, then equip the freshest unbroken elytra from the inventory.
      // This runs mid-flight too: a broken elytra up here kills just the same,
      // and plenty of anarchy servers never spawn Phantoms to repair with.
      if (equipSlot == EquipmentSlot.CHEST
          && elytraSaver.getValue()
          && mc.player.getItemBySlot(equipSlot).is(Items.ELYTRA)
          && getDurabilityPercent(mc.player.getItemBySlot(equipSlot))
              <= elytraDepletion.getValue()) {
        if (!mc.player.isFallFlying() && hasEmptyInventorySlot()) {
          quickMoveArmor(containerSlot);
          sleep = true;
        }
        equipFreshElytra();
        continue;
      }

      // Never touch a healthy chest slot mid-flight: stripping a worn elytra kills.
      if (equipSlot == EquipmentSlot.CHEST && mc.player.isFallFlying()) {
        continue;
      }

      ItemStack current = mc.player.getItemBySlot(equipSlot);
      boolean isEmpty = current.isEmpty();

      boolean saveCurrent =
          !isEmpty
              && armorSaver.getValue()
              && current.getCount() == 1
              && getDurabilityPercent(current) <= depletion.getValue();

      boolean shouldReplace = isEmpty || saveCurrent;

      if (!shouldReplace && !isEmpty) {
        int currentScore = getArmorScore(current, equipSlot);
        for (ArmorCandidate candidate : candidates) {
          if (candidate.slot() == equipSlot && candidate.score() > currentScore) {
            shouldReplace = true;
            break;
          }
        }
      }

      if (!shouldReplace) continue;

      for (ArmorCandidate candidate : candidates) {
        if (candidate.slot() != equipSlot) continue;
        if (saveCurrent && getDurabilityPercent(candidate.stack()) <= depletion.getValue())
          continue;

        if (candidate.stack().getCount() > 1 && stackArmor.getValue()) {
          swapStack(candidate.containerSlot(), containerSlot);
        } else {
          swap(candidate.containerSlot(), containerSlot);
        }
        break;
      }
    }
  }

  private void equipFreshElytra() {
    int bestSlot = -1;
    int bestRemaining = 0;
    for (int i = 9; i < 45; i++) {
      ItemStack stack = mc.player.containerMenu.getSlot(i).getItem();
      if (stack.isEmpty() || !stack.is(Items.ELYTRA)) continue;
      int remaining = stack.getMaxDamage() - stack.getDamageValue();
      if (remaining > bestRemaining) {
        bestRemaining = remaining;
        bestSlot = i;
      }
    }
    if (bestSlot != -1) {
      quickMoveArmor(bestSlot);
      sleep = true;
    }
  }

  private void swap(int source, int target) {
    int containerId = mc.player.containerMenu.containerId;

    if (mc.player.containerMenu.getSlot(target).getItem().isEmpty()) {
      mc.gameMode.handleContainerInput(
          containerId, source, 0, ContainerInput.QUICK_MOVE, mc.player);
    } else {
      boolean hasEmpty = false;
      for (int i = 9; i < 45; i++) {
        if (mc.player.containerMenu.getSlot(i).getItem().isEmpty()) {
          hasEmpty = true;
          break;
        }
      }

      if (hasEmpty) {
        mc.gameMode.handleContainerInput(
            containerId, target, 0, ContainerInput.QUICK_MOVE, mc.player);
        mc.gameMode.handleContainerInput(
            containerId, source, 0, ContainerInput.QUICK_MOVE, mc.player);
      } else {
        mc.gameMode.handleContainerInput(containerId, source, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(containerId, target, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(containerId, source, 0, ContainerInput.PICKUP, mc.player);
      }
    }
    sleep = true;
  }

  private void swapStack(int source, int target) {
    int containerId = mc.player.containerMenu.containerId;

    if (!mc.player.containerMenu.getSlot(target).getItem().isEmpty()) {
      mc.gameMode.handleContainerInput(
          containerId, target, 0, ContainerInput.QUICK_MOVE, mc.player);
    }

    int hotbarIndex = swapSlot.getValue() - 1;
    mc.gameMode.handleContainerInput(
        containerId, source, hotbarIndex, ContainerInput.SWAP, mc.player);

    int prevSelected = mc.player.getInventory().getSelectedSlot();
    mc.player.getInventory().setSelectedSlot(hotbarIndex);

    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);

    mc.player.getInventory().setSelectedSlot(prevSelected);

    mc.gameMode.handleContainerInput(
        containerId, source, hotbarIndex, ContainerInput.SWAP, mc.player);

    sleep = true;
  }

  private void quickMoveArmor(int containerSlot) {
    int containerId = mc.player.containerMenu.containerId;
    mc.gameMode.handleContainerInput(
        containerId, containerSlot, 0, ContainerInput.QUICK_MOVE, mc.player);
  }

  private List<ArmorCandidate> findArmorCandidates() {
    List<ArmorCandidate> candidates = new ArrayList<>();

    for (int i = 9; i < 45; i++) {
      ItemStack stack = mc.player.containerMenu.getSlot(i).getItem();
      if (stack.isEmpty()) continue;

      EquipmentSlot slot = getArmorSlot(stack);
      if (slot == null) continue;
      if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) continue;

      int score = getArmorScore(stack, slot);
      candidates.add(new ArmorCandidate(stack, slot, i, score));
    }

    candidates.sort(Comparator.comparingInt(ArmorCandidate::score).reversed());
    return candidates;
  }

  private EquipmentSlot getArmorSlot(ItemStack stack) {
    Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
    if (equippable != null) {
      return equippable.slot();
    }
    return null;
  }

  private int getArmorScore(ItemStack stack, EquipmentSlot slot) {
    int defense = getDefense(stack);
    int enchantScore = getEnchantmentScore(stack);
    return defense * 100 + enchantScore;
  }

  private int getDefense(ItemStack stack) {
    int[] defense = {0};
    stack.forEachModifier(
        EquipmentSlotGroup.ANY,
        (attribute, modifier, display) -> {
          if (attribute.is(Attributes.ARMOR)) {
            defense[0] += (int) modifier.amount();
          }
        });
    return defense[0];
  }

  private int getEnchantmentScore(ItemStack stack) {
    ItemEnchantments enchantments = stack.getEnchantments();
    int score = 0;
    for (var entry : enchantments.entrySet()) {
      Holder<Enchantment> ench = entry.getKey();
      int level = entry.getIntValue();
      if (ench.is(Enchantments.PROTECTION)) score += level * 10;
      else if (ench.is(Enchantments.BLAST_PROTECTION)) score += level * 8;
      else if (ench.is(Enchantments.PROJECTILE_PROTECTION)) score += level * 8;
      else if (ench.is(Enchantments.FIRE_PROTECTION)) score += level * 7;
      else if (ench.is(Enchantments.THORNS)) score += level * 5;
      else if (ench.is(Enchantments.UNBREAKING)) score += level * 4;
      else if (ench.is(Enchantments.MENDING)) score += level * 3;
      else score += level * 2;
    }
    return score;
  }

  private boolean hasEnchantment(ItemStack stack, ResourceKey<Enchantment> ench) {
    ItemEnchantments enchantments = stack.getEnchantments();
    for (var entry : enchantments.entrySet()) {
      if (entry.getKey().is(ench)) return true;
    }
    return false;
  }

  private int getDurabilityPercent(ItemStack stack) {
    if (stack.getMaxDamage() == 0) return 100;
    return (int)
        ((float) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage() * 100);
  }

  private boolean hasEmptyInventorySlot() {
    for (int i = 9; i < 45; i++) {
      if (mc.player.containerMenu.getSlot(i).getItem().isEmpty()) return true;
    }
    return false;
  }

  private boolean hasNearbyEnemies() {
    for (var entity : mc.level.entitiesForRendering()) {
      if (entity instanceof Player p && p != mc.player) {
        if (!Exeter.getInstance().getFriendManager().isTargetable(p.getName().getString())) {
          continue;
        }
        if (p.distanceTo(mc.player) <= 6.0f) return true;
      }
      if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal) {
        if (entity.distanceTo(mc.player) <= 12.0f) return true;
      }
    }
    return false;
  }

  private boolean maceDiveRunning() {
    var module =
        me.friendly.exeter.core.Exeter.getInstance()
            .getModuleManager()
            .getModuleByAlias("macedive");
    return module instanceof me.friendly.exeter.module.ToggleableModule toggleable
        && toggleable.isRunning();
  }

  private static EquipmentSlot slotFromIndex(int index) {
    return switch (index) {
      case 0 -> EquipmentSlot.HEAD;
      case 1 -> EquipmentSlot.CHEST;
      case 2 -> EquipmentSlot.LEGS;
      case 3 -> EquipmentSlot.FEET;
      default -> EquipmentSlot.MAINHAND;
    };
  }

  public StopWatch getRightClickTimer() {
    return rightClickTimer;
  }

  private record ArmorCandidate(
      ItemStack stack, EquipmentSlot slot, int containerSlot, int score) {}
}
