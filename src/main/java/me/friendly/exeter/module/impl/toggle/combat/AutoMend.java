package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.StopWatch;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.Vec3;

public class AutoMend extends ToggleableModule {

  private final NumberProperty<Integer> delay = new NumberProperty<>(0, 0, 10, "Delay");
  private final NumberProperty<Integer> minDamage = new NumberProperty<>(50, 1, 100, "Min Damage");
  private final NumberProperty<Integer> repairTo = new NumberProperty<>(90, 1, 100, "Repair To");
  private final Property<Boolean> takeOff = new Property<>(true, "TakeOff");
  private final NumberProperty<Integer> takeOffDelay = new NumberProperty<>(0, 0, 10, "TakeOff Delay");
  private final Property<Boolean> healthCheck = new Property<>(true, "Health Check");
  private final NumberProperty<Integer> minHealth = new NumberProperty<>(16, 0, 36, "Min Health");
  private final Property<Boolean> enemyCheck = new Property<>(true, "Enemy Check");

  private final StopWatch timer = new StopWatch();
  private final StopWatch takeOffTimer = new StopWatch();
  private int toMendFlags;

  private static final Minecraft mc = Minecraft.getInstance();

  private final Listener<TickEvent> tickListener = new Listener<TickEvent>("automend_tick") {
    @Override
    public void call(TickEvent event) {
      if (event.getStage() != Stage.PRE) return;
      onTick();
    }
  };

  public AutoMend() {
    super("AutoMend", new String[]{"automend", "auto-mend"}, 0xFF55FF, ModuleType.COMBAT);
    setDescription("Uses XP bottles to mend your armor via Mending enchantment.");
    offerProperties(delay, minDamage, repairTo, takeOff, takeOffDelay, healthCheck, minHealth,
        enemyCheck);
    listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    toMendFlags = 0;
  }

  private void onTick() {
    if (mc.player == null || mc.level == null || mc.gameMode == null) return;
    if (mc.player.isDeadOrDying() || mc.player.tickCount < 10) return;
    if (mc.player.containerMenu != mc.player.inventoryMenu) return;

    if (healthCheck.getValue()
        && mc.player.getHealth() + mc.player.getAbsorptionAmount() < minHealth.getValue()) {
      sendDisableMessage("Low health");
      setRunning(false);
      return;
    }

    if (enemyCheck.getValue() && hasNearbyEnemies()) {
      sendDisableMessage("Players nearby");
      setRunning(false);
      return;
    }

    int xpSlot = findXPSlot();
    if (xpSlot == -1) {
      sendDisableMessage("No XP bottle in hotbar");
      setRunning(false);
      return;
    }

    if (checkFinished()) {
      sendDisableMessage("All armor mended");
      setRunning(false);
      return;
    }

    if (!timer.hasPassed((long) delay.getValue() * 50)) return;
    timer.reset();

    toMendFlags = 0;
    List<ItemStack> armors = getEquippedArmor();

    for (int i = 0; i < armors.size(); i++) {
      ItemStack itemStack = armors.get(i);
      if (itemStack.isEmpty()) continue;
      if (!hasEnchantment(itemStack, Enchantments.MENDING)) continue;

      int durabilityPercent = getDurabilityPercent(itemStack);
      if (durabilityPercent >= repairTo.getValue()) continue;
      if (durabilityPercent <= minDamage.getValue()) {
        toMendFlags |= (1 << i);
      }
    }

    if (toMendFlags > 0) {
      lookDown();
      useXPBottle(xpSlot);

      if (takeOff.getValue()) {
        takeOffRepaired();
      }
    }
  }

  private void lookDown() {
    mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(
        mc.player.getYRot(), 90.0f, mc.player.onGround(), false));
    mc.player.setXRot(90.0f);
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
      mc.gameMode.handleContainerInput(containerId, containerSlot, 0, ContainerInput.QUICK_MOVE, mc.player);
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

  private boolean hasNearbyEnemies() {
    Vec3 pos = mc.player.position();
    for (var entity : mc.level.entitiesForRendering()) {
      if (entity instanceof Player p && p != mc.player) {
        if (p.distanceTo(mc.player) <= 6.0f) return true;
      }
    }
    return false;
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
    return (int) ((float) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage() * 100);
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
    if (mc.player != null) {
      mc.player.sendSystemMessage(
          Component.literal("§c[AutoMend] " + reason + " - disabling"));
    }
  }
}
