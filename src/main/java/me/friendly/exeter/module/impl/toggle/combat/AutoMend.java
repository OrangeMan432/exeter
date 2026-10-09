package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import me.friendly.exeter.util.StopWatch;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class AutoMend extends ToggleableModule {

  private final NumberProperty<Integer> delay = new NumberProperty<>(0, 0, 10, "Delay");
  private final NumberProperty<Integer> minDamage = new NumberProperty<>(50, 1, 100, "Min Damage");
  private final NumberProperty<Integer> repairTo = new NumberProperty<>(90, 1, 100, "Repair To");
  private final Property<Boolean> takeOff = new Property<>(true, "TakeOff");
  private final NumberProperty<Integer> takeOffDelay =
      new NumberProperty<>(0, 0, 10, "TakeOff Delay");
  private final Property<Boolean> healthCheck = new Property<>(true, "Health Check");
  private final NumberProperty<Integer> minHealth = new NumberProperty<>(16, 0, 36, "Min Health");
  private final Property<Boolean> enemyCheck = new Property<>(true, "Enemy Check");
  private final Property<Boolean> disableOnComplete = new Property<>(true, "Disable On Complete");

  private final StopWatch timer = new StopWatch();
  private final StopWatch takeOffTimer = new StopWatch();
  private int toMendFlags;
  private int activeMendFlags;
  private boolean wasFinished;
  private boolean seeded;

  private static final Minecraft mc = Minecraft.getInstance();

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("automend_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          onTick();
        }
      };

  public AutoMend() {
    super("AutoMend", new String[] {"automend", "auto-mend"}, 0xFF55FF, ModuleType.COMBAT);
    setDescription("Uses XP bottles to mend your armor via Mending enchantment.");
    offerProperties(
        delay,
        minDamage,
        repairTo,
        takeOff,
        takeOffDelay,
        healthCheck,
        minHealth,
        enemyCheck,
        disableOnComplete);
    delay.setDescription("Ticks between XP bottle throws.");
    minDamage.setDescription("Start mending armor at or below this durability percent.");
    repairTo.setDescription("Stop mending armor once it reaches this durability.");
    takeOff.setDescription("Unequip armor once it is repaired.");
    takeOffDelay.setDescription("Ticks between unequipping repaired pieces.");
    healthCheck.setDescription("Stop when your health drops too low.");
    minHealth.setDescription("Stop below this health.");
    enemyCheck.setDescription("Stop while enemies are nearby.");
    disableOnComplete.setDescription("Turn off once all armor is repaired.");
    listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    toMendFlags = 0;
    activeMendFlags = 0;
    wasFinished = false;
    seeded = false;
    debug("enabled");
    DebugLogger.get()
        .logFile(
            getLabel(),
            "settings: disableOnComplete="
                + disableOnComplete.getValue()
                + " minDamage="
                + minDamage.getValue()
                + " repairTo="
                + repairTo.getValue()
                + " takeOff="
                + takeOff.getValue()
                + " healthCheck="
                + healthCheck.getValue()
                + " enemyCheck="
                + enemyCheck.getValue());
  }

  private void onTick() {
    if (mc.player == null || mc.level == null || mc.gameMode == null) return;
    if (mc.player.isDeadOrDying() || mc.player.tickCount < 10) return;
    if (mc.player.containerMenu != mc.player.inventoryMenu) return;

    if (!seeded) {
      seeded = true;
      seedActiveFlags();
    }

    if (healthCheck.getValue()) {
      float effectiveHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
      if (effectiveHealth < minHealth.getValue()) {
        sendDisableMessage(
            "Low health ("
                + String.format("%.1f", effectiveHealth)
                + " < "
                + minHealth.getValue()
                + ")");
        setRunning(false);
        return;
      }
    }

    if (enemyCheck.getValue()) {
      List<Player> nearby = nearbyPlayers();
      if (!nearby.isEmpty()) {
        StringBuilder reason = new StringBuilder("Players nearby (");
        for (int i = 0; i < nearby.size(); i++) {
          if (i > 0) reason.append(", ");
          Player p = nearby.get(i);
          reason
              .append(p.getName().getString())
              .append(" ")
              .append(String.format("%.1f", p.distanceTo(mc.player)))
              .append("m");
        }
        reason.append(")");
        sendDisableMessage(reason.toString());
        setRunning(false);
        return;
      }
    }

    int xpSlot = findXPSlot();
    if (xpSlot == -1) {
      sendDisableMessage("No XP bottle in hotbar");
      setRunning(false);
      return;
    }

    if (checkFinished()) {
      if (!wasFinished) {
        wasFinished = true;
        debug(
            "All armor mended ("
                + armorSummary()
                + (disableOnComplete.getValue() ? " - disabling" : " - monitoring"));
      }
      if (disableOnComplete.getValue()) {
        sendDisableMessage("All armor mended (" + armorSummary() + ")");
        setRunning(false);
      }
      return;
    }
    wasFinished = false;

    if (!timer.hasPassed((long) delay.getValue() * 50)) return;
    timer.reset();

    toMendFlags = 0;
    List<ItemStack> armors = getEquippedArmor();

    for (int i = 0; i < armors.size(); i++) {
      ItemStack itemStack = armors.get(i);
      if (itemStack.isEmpty() || !hasEnchantment(itemStack, Enchantments.MENDING)) {
        activeMendFlags &= ~(1 << i);
        continue;
      }

      int durabilityPercent = getDurabilityPercent(itemStack);
      if (durabilityPercent >= repairTo.getValue()) {
        activeMendFlags &= ~(1 << i);
        continue;
      }
      if (durabilityPercent <= minDamage.getValue() && (activeMendFlags & (1 << i)) == 0) {
        activeMendFlags |= (1 << i);
        debug(slotFromIndex(i) + " latched at " + durabilityPercent + "%");
      }
      if ((activeMendFlags & (1 << i)) != 0) {
        toMendFlags |= (1 << i);
      }
    }

    if (toMendFlags > 0) {
      float yaw = mc.player.getYRot();
      float pitch = mc.player.getXRot();
      PlayerUtil.setRotation(yaw, 90.0f);
      useXPBottle(xpSlot);
      PlayerUtil.restoreRotation(yaw, pitch);
      debug("used XP bottle from slot " + xpSlot);

      if (takeOff.getValue()) {
        takeOffRepaired();
      }
    }
  }

  private void useXPBottle(int hotbarSlot) {
    int prevSelected = mc.player.getInventory().getSelectedSlot();
    mc.player.getInventory().setSelectedSlot(hotbarSlot);
    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
    mc.player.getInventory().setSelectedSlot(prevSelected);
  }

  private void takeOffRepaired() {
    for (int i = 0; i < 4; i++) {
      EquipmentSlot slot = slotFromIndex(i);
      ItemStack armor = mc.player.getItemBySlot(slot);
      if (armor.isEmpty()) continue;

      int durabilityPercent = getDurabilityPercent(armor);
      if (durabilityPercent < repairTo.getValue()) continue;

      if (!hasEmptyInventorySlot()) return;
      if (!takeOffTimer.hasPassed((long) takeOffDelay.getValue() * 50)) return;
      takeOffTimer.reset();

      int containerSlot = 5 + i;
      int containerId = mc.player.containerMenu.containerId;
      mc.gameMode.handleContainerInput(
          containerId, containerSlot, 0, ContainerInput.QUICK_MOVE, mc.player);
      debug("took off repaired " + slot + " (" + durabilityPercent + "%)");
      return;
    }
  }

  private boolean checkFinished() {
    for (int i = 0; i < 4; i++) {
      ItemStack armor = mc.player.getItemBySlot(slotFromIndex(i));
      if (armor.isEmpty()) continue;
      if (hasEnchantment(armor, Enchantments.MENDING)) {
        if (getDurabilityPercent(armor) < repairTo.getValue()) {
          return false;
        }
      }
    }
    return true;
  }

  private void seedActiveFlags() {
    for (int i = 0; i < 4; i++) {
      ItemStack armor = mc.player.getItemBySlot(slotFromIndex(i));
      if (armor.isEmpty() || !hasEnchantment(armor, Enchantments.MENDING)) continue;
      if (getDurabilityPercent(armor) < repairTo.getValue()) {
        activeMendFlags |= (1 << i);
      }
    }
    if (activeMendFlags != 0) {
      debug(
          "seeded "
              + Integer.bitCount(activeMendFlags)
              + " piece(s) below "
              + repairTo.getValue()
              + "%");
    }
  }

  private String armorSummary() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 4; i++) {
      ItemStack armor = mc.player.getItemBySlot(slotFromIndex(i));
      if (armor.isEmpty()) continue;
      if (sb.length() > 0) sb.append(", ");
      sb.append(slotFromIndex(i)).append(" ").append(getDurabilityPercent(armor)).append("%");
    }
    return sb.toString();
  }

  private List<ItemStack> getEquippedArmor() {
    List<ItemStack> armors = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      armors.add(mc.player.getItemBySlot(slotFromIndex(i)));
    }
    return armors;
  }

  private int findXPSlot() {
    for (int i = 0; i < 9; i++) {
      if (mc.player.getInventory().getItem(i).getItem() == Items.EXPERIENCE_BOTTLE) {
        return i;
      }
    }
    return -1;
  }

  private List<Player> nearbyPlayers() {
    List<Player> nearby = new ArrayList<>();
    for (var entity : mc.level.entitiesForRendering()) {
      if (entity instanceof Player p && p != mc.player) {
        if (!Exeter.getInstance().getFriendManager().isTargetable(p.getName().getString())) {
          continue;
        }
        if (p.distanceTo(mc.player) <= 6.0f) nearby.add(p);
      }
    }
    return nearby;
  }

  private boolean hasEmptyInventorySlot() {
    for (int i = 9; i < 45; i++) {
      if (mc.player.containerMenu.getSlot(i).getItem().isEmpty()) return true;
    }
    return false;
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

  private static EquipmentSlot slotFromIndex(int index) {
    return switch (index) {
      case 0 -> EquipmentSlot.HEAD;
      case 1 -> EquipmentSlot.CHEST;
      case 2 -> EquipmentSlot.LEGS;
      case 3 -> EquipmentSlot.FEET;
      default -> EquipmentSlot.MAINHAND;
    };
  }

  private void sendDisableMessage(String reason) {
    DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, reason + " - disabling");
  }

  private void debug(String message) {
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, message);
  }
}
