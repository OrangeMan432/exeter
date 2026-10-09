package me.friendly.exeter.module.impl.toggle.combat;

import java.util.HashMap;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.state.BlockState;

public class AutoPot extends ToggleableModule {

  private final Property<Boolean> hp = new Property<Boolean>(false, "Health Potion");
  private final NumberProperty<Integer> health = new NumberProperty<Integer>(16, 0, 20, "Health");
  private final Property<Boolean> equal = new Property<Boolean>(false, "Equal");
  private final Property<Boolean> predict = new Property<Boolean>(false, "Predict");
  private final NumberProperty<Double> times =
      new NumberProperty<Double>(1.0, 0.0, 5.0, "Time Seconds");
  private final NumberProperty<Integer> predictHpDelay =
      new NumberProperty<Integer>(50, 0, 1000, "Predict HP Delay");
  private final NumberProperty<Integer> healthSlot =
      new NumberProperty<Integer>(1, 1, 9, "Health Slot");
  private final NumberProperty<Integer> hpDelay =
      new NumberProperty<Integer>(50, 0, 1000, "Health Delay");

  private final Property<Boolean> speedPot = new Property<Boolean>(false, "Swiftness");
  private final NumberProperty<Integer> timeLeft =
      new NumberProperty<Integer>(5, 0, 30, "Time Left");
  private final NumberProperty<Integer> swiftnessSlot =
      new NumberProperty<Integer>(1, 1, 9, "Swiftness Slot");
  private final NumberProperty<Integer> speedDelay =
      new NumberProperty<Integer>(50, 0, 1000, "Swiftness Delay");

  private final Property<Boolean> only = new Property<Boolean>(true, "On Ground Only");
  private final Property<Boolean> silentSwitch = new Property<Boolean>(true, "Packet Switch");

  private final NumberProperty<Integer> badDelay =
      new NumberProperty<Integer>(10, 0, 30, "Bad Pot Delay");
  private final NumberProperty<Double> range = new NumberProperty<Double>(4.0, 0.0, 10.0, "Range");
  private final NumberProperty<Integer> badSlot =
      new NumberProperty<Integer>(1, 1, 9, "Badpot Slot");
  private final Property<Boolean> weak = new Property<Boolean>(false, "Weakness");
  private final Property<Boolean> doJump = new Property<Boolean>(false, "Jump Boost");
  private final Property<Boolean> poison = new Property<Boolean>(false, "Poison");
  private final Property<Boolean> slow = new Property<Boolean>(false, "Slowness");

  private final HashMap<Integer, Long> weaknessTime = new HashMap<>();
  private final HashMap<Integer, Long> jumpBoostTime = new HashMap<>();
  private final HashMap<Integer, Long> poisonTime = new HashMap<>();
  private final HashMap<Integer, Long> slownessTime = new HashMap<>();

  private long hpTimer;
  private long hpPredictTimer;
  private long speedTimer;
  private long lastSpeedThrow;
  private long badPotTimer;

  private int potionSlot;
  private int potSlot;
  private double lastHealth = 36.0;
  private boolean preHp;

  public AutoPot() {
    super("AutoPot", new String[] {"autopot", "auto-pot"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Automatically throws healing and speed potions.");

    offerProperties(
        hp,
        health,
        equal,
        predict,
        times,
        predictHpDelay,
        healthSlot,
        hpDelay,
        speedPot,
        timeLeft,
        swiftnessSlot,
        speedDelay,
        only,
        silentSwitch,
        badDelay,
        range,
        badSlot,
        weak,
        doJump,
        poison,
        slow);
    hp.setDescription("Throw healing potions when health is low.");
    health.setDescription("Throw a healing potion below this health.");
    equal.setDescription("Also throw when health exactly equals the threshold.");
    predict.setDescription("Throw early when health is about to drop.");
    times.setDescription("How far ahead to project falling health.");
    predictHpDelay.setDescription("Cooldown between predicted heals, in milliseconds.");
    healthSlot.setDescription("Hotbar slot healing potions are moved into.");
    hpDelay.setDescription("Cooldown between healing throws, in milliseconds.");
    speedPot.setDescription("Throw swiftness potions when the effect runs low.");
    timeLeft.setDescription("Re-throw swiftness when fewer seconds of effect remain.");
    swiftnessSlot.setDescription("Hotbar slot swiftness potions are moved into.");
    speedDelay.setDescription("Cooldown between swiftness throws, in milliseconds.");
    only.setDescription("Only throw while standing over solid ground.");
    silentSwitch.setDescription("Return to the previous slot after throwing.");
    badDelay.setDescription("Seconds between offensive throws.");
    range.setDescription("Throw offensive potions at enemies within this many blocks.");
    badSlot.setDescription("Hotbar slot offensive potions are moved into.");
    weak.setDescription("Throw weakness potions at nearby enemies.");
    doJump.setDescription("Throw leaping potions at nearby enemies.");
    poison.setDescription("Throw poison potions at nearby enemies.");
    slow.setDescription("Throw slowness potions at nearby enemies.");

    listeners.add(
        new Listener<TickEvent>("auto_pot_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });

    listeners.add(
        new Listener<PacketEvent>("auto_pot_packet") {
          @Override
          public void call(PacketEvent event) {
            onPacket(event);
          }
        });
  }

  @Override
  protected void onEnable() {
    weaknessTime.clear();
    jumpBoostTime.clear();
    poisonTime.clear();
    slownessTime.clear();
    hpTimer = 0;
    hpPredictTimer = 0;
    speedTimer = 0;
    lastSpeedThrow = 0;
    badPotTimer = 0;
    potionSlot = -1;
    potSlot = -1;
    lastHealth = 36.0;
    preHp = false;
    super.onEnable();
  }

  @Override
  protected void onDisable() {
    potionSlot = -1;
    potSlot = -1;
    super.onDisable();
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying())
      return;

    long now = System.currentTimeMillis();
    for (Player player : minecraft.level.players()) {
      int id = player.getId();
      if (weaknessTime.containsKey(id) && weaknessTime.get(id) <= now) weaknessTime.remove(id);
      if (jumpBoostTime.containsKey(id) && jumpBoostTime.get(id) <= now) jumpBoostTime.remove(id);
      if (poisonTime.containsKey(id) && poisonTime.get(id) <= now) poisonTime.remove(id);
      if (slownessTime.containsKey(id) && slownessTime.get(id) <= now) slownessTime.remove(id);
    }

    if (!canThrow()) return;

    if (potionSlot == -1) {
      potionSlot = getPotion();
    }

    if (potSlot == -1) {
      potSlot = getBadPot();
    }

    if (potionSlot != -1 || potSlot != -1) {
      if (potionSlot > 8) {
        if (minecraft.gui.screen() instanceof AbstractContainerScreen
            && !(minecraft.gui.screen() instanceof InventoryScreen)) {
          return;
        }
        int finalSlot =
            potionSlot == getPotionSlot("swiftness")
                ? swiftnessSlot.getValue() - 1
                : healthSlot.getValue() - 1;
        minecraft.gameMode.handleContainerInput(
            minecraft.player.containerMenu.containerId,
            potionSlot,
            finalSlot,
            ContainerInput.SWAP,
            minecraft.player);
        potionSlot = finalSlot;
      }

      if (potSlot > 8) {
        if (minecraft.gui.screen() instanceof AbstractContainerScreen
            && !(minecraft.gui.screen() instanceof InventoryScreen)) {
          return;
        }
        minecraft.gameMode.handleContainerInput(
            minecraft.player.containerMenu.containerId,
            potSlot,
            badSlot.getValue() - 1,
            ContainerInput.SWAP,
            minecraft.player);
        potSlot = badSlot.getValue() - 1;
      }

      int slot = potionSlot == -1 ? potSlot : potionSlot;
      throwPotion(slot);
    }
  }

  private void throwPotion(int slot) {
    if (slot < 0 || slot > 8) return;
    PlayerUtil.swapTo(slot);

    float origYaw = minecraft.player.getYRot();
    float origPitch = minecraft.player.getXRot();

    minecraft.player.setYRot(origYaw);
    minecraft.player.setXRot(90f);

    minecraft
        .getConnection()
        .send(
            new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(
                origYaw, 90f, minecraft.player.onGround(), false));
    minecraft
        .getConnection()
        .send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, origYaw, 90f));

    minecraft.player.setYRot(origYaw);
    minecraft.player.setXRot(origPitch);

    if (silentSwitch.getValue()) {
      PlayerUtil.swapBack();
    }

    potionSlot = -1;
    potSlot = -1;
  }

  private int getPotion() {
    if (hp.getValue()) {
      if (healthCheck(health.getValue()) && passedMs(hpTimer, hpDelay.getValue())) {
        preHp = false;
        hpTimer = System.currentTimeMillis();
        int slot = getPotionSlot("healing");
        if (slot != -1) return slot;
      }

      if (predict.getValue()) {
        healthPredict();
      }

      if (preHp && passedMs(hpPredictTimer, predictHpDelay.getValue())) {
        preHp = false;
        hpPredictTimer = System.currentTimeMillis();
        int slot = getPotionSlot("healing");
        if (slot != -1) return slot;
      }
    }

    if (speedPot.getValue()
        && (!minecraft.player.hasEffect(MobEffects.SPEED)
            || minecraft.player.getEffect(MobEffects.SPEED).getDuration()
                <= timeLeft.getValue() * 20)
        && passedMs(speedTimer, speedDelay.getValue())
        && passedMs(lastSpeedThrow, 1000)) {
      speedTimer = System.currentTimeMillis();
      lastSpeedThrow = System.currentTimeMillis();
      return getPotionSlot("swiftness");
    }

    return -1;
  }

  private int getBadPot() {
    if (passedS(badPotTimer, badDelay.getValue())) {
      badPotTimer = System.currentTimeMillis();
      for (Player player : minecraft.level.players()) {
        if (player != minecraft.player
            && minecraft.getConnection().getPlayerInfo(player.getUUID()) != null
            && !basicChecks(player)
            && !(minecraft.player.distanceTo(player) > range.getValue())) {
          if (weak.getValue() && !weaknessTime.containsKey(player.getId())) {
            int slot = getPotionSlot("weakness");
            if (slot != -1) return slot;
          }

          if (doJump.getValue() && !jumpBoostTime.containsKey(player.getId())) {
            int slot = getPotionSlot("leaping");
            if (slot != -1) return slot;
          }

          if (poison.getValue() && !poisonTime.containsKey(player.getId())) {
            int slot = getPotionSlot("poison");
            if (slot != -1) return slot;
          }

          if (slow.getValue() && !slownessTime.containsKey(player.getId())) {
            int slot = getPotionSlot("slowness");
            if (slot != -1) return slot;
          }
        }
      }
    }
    return -1;
  }

  private boolean basicChecks(Player player) {
    return !player.isAlive()
        || !Exeter.getInstance().getFriendManager().isTargetable(player.getName().getString());
  }

  private boolean healthCheck(double value) {
    return minecraft.player.getHealth() < value
        || (equal.getValue() && minecraft.player.getHealth() == value);
  }

  private void healthPredict() {
    double healthNow = minecraft.player.getHealth() + minecraft.player.getAbsorptionAmount();
    if (healthNow == 36.0) {
      lastHealth = 36.0;
    }

    double change = healthNow - lastHealth;
    if (change < 0.0) {
      lastHealth = healthNow;
      healthNow += change * times.getValue();
      preHp = healthNow < health.getValue() || (equal.getValue() && healthNow == health.getValue());
    }
  }

  private int getPotionSlot(String potion) {
    for (int i = 0; i < 36; i++) {
      ItemStack stack = minecraft.player.getInventory().getItem(i);
      if (stack.getItem() == Items.SPLASH_POTION) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
          Holder<Potion> holder = contents.potion().orElse(null);
          if (holder != null
              && holder.unwrapKey().isPresent()
              && holder.unwrapKey().get().identifier().getPath().contains(potion)) {
            return i;
          }
        }
      }
    }
    return -1;
  }

  private boolean canThrow() {
    if (only.getValue() || minecraft.player.isInLava() || minecraft.player.isInWater()) {
      double eyeX = minecraft.player.getX();
      double eyeZ = minecraft.player.getZ();
      int feetY = minecraft.player.getBlockY();
      for (int dy = 1; dy <= 2; dy++) {
        BlockPos pos = BlockPos.containing(eyeX, feetY - dy, eyeZ);
        BlockState state = minecraft.level.getBlockState(pos);
        if (!state.isAir() && !state.getCollisionShape(minecraft.level, pos).isEmpty()) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  private boolean passedMs(long start, int ms) {
    return System.currentTimeMillis() - start >= ms;
  }

  private boolean passedS(long start, int seconds) {
    return System.currentTimeMillis() - start >= seconds * 1000L;
  }

  private void onPacket(PacketEvent event) {
    if (event.getPacket() instanceof ClientboundRemoveEntitiesPacket packet) {
      for (int id : packet.entityIds()) {
        weaknessTime.remove(id);
        jumpBoostTime.remove(id);
        poisonTime.remove(id);
        slownessTime.remove(id);
      }
    }

    if (event.getPacket() instanceof ClientboundEntityEventPacket packet
        && packet.getEventId() == 35) {
      int id = packet.getEntity(minecraft.level).getId();
      weaknessTime.remove(id);
      jumpBoostTime.remove(id);
      poisonTime.remove(id);
      slownessTime.remove(id);
    }
  }
}
