package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Items;

/**
 * Automatically fires fireworks while flying with an elytra.
 *
 * <p>Refill mode fires whenever no live rocket is boosting the player anymore. Delay mode fires on
 * a fixed tick interval instead.
 */
public class AutoFirework extends ToggleableModule {

  public enum FireworkMode {
    REFILL,
    DELAY
  }

  private final EnumProperty<FireworkMode> mode =
      new EnumProperty<>(FireworkMode.REFILL, "Mode", "mode");
  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(30, 1, 200, "Delay", "delay");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> switchBack = new Property<Boolean>(true, "Switch Back");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  private int tickCounter;
  private int refillGrace;
  private boolean wasFlying;
  private boolean outNotified;

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("autofirework_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          AutoFirework.this.onTick();
        }
      };

  public AutoFirework() {
    super(
        "AutoFirework",
        new String[] {"autofirework", "elytraboost"},
        0xFFFF55,
        ModuleType.MISCELLANEOUS);
    setDescription("Automatically fires fireworks while flying with an elytra.");
    offerProperties(mode, delay, autoSwitch, switchBack, swingHand);
    mode.setDescription("When to fire the next firework.");
    delay.setDescription("Ticks between fireworks in Delay mode.");
    autoSwitch.setDescription("Switch to fireworks automatically.");
    switchBack.setDescription("Switch back after firing.");
    swingHand.setDescription("Swing your hand when firing.");
    this.listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
    refillGrace = 0;
    wasFlying = false;
    outNotified = false;
    DebugLogger.get()
        .log(
            getLabel(),
            DebugLogger.Level.INFO,
            "Enabled (mode=" + mode.getValue() + ", delay=" + delay.getValue() + ")");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    tickCounter = 0;
    refillGrace = 0;
    wasFlying = false;
    outNotified = false;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying()) {
      return;
    }
    if (maceDiveActive()) {
      return;
    }
    if (!minecraft.player.isFallFlying()) {
      wasFlying = false;
      return;
    }
    if (!wasFlying) {
      wasFlying = true;
      // Delay mode fires immediately for takeoff, then waits the delay between throws.
      tickCounter = mode.getValue() == FireworkMode.DELAY ? delay.getValue() : 0;
      DebugLogger.get().logFile(getLabel(), "Flight detected, starting fireworks");
    }
    if (refillGrace > 0) {
      refillGrace--;
      return;
    }

    int slot =
        PlayerUtil.findInHotbar(stack -> !stack.isEmpty() && stack.is(Items.FIREWORK_ROCKET));
    if (slot == -1) {
      if (!outNotified) {
        outNotified = true;
        DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "Out of fireworks in hotbar");
      }
      return;
    }
    outNotified = false;

    boolean shouldFire;
    if (mode.getValue() == FireworkMode.DELAY) {
      shouldFire = ++tickCounter >= delay.getValue();
    } else {
      shouldFire = !hasActiveRocket();
    }
    if (!shouldFire) {
      return;
    }
    tickCounter = 0;
    if (mode.getValue() == FireworkMode.REFILL) {
      // Spawn packets take a few ticks to arrive; hold fire so one rocket isn't stacked.
      refillGrace = 10;
    }
    fire(slot);
  }

  /** Stands down while MaceDive runs so the two never fight over flight. */
  private boolean maceDiveActive() {
    var module =
        me.friendly.exeter.core.Exeter.getInstance()
            .getModuleManager()
            .getModuleByAlias("macedive");
    return module instanceof me.friendly.exeter.module.ToggleableModule toggleable
        && toggleable.isRunning();
  }

  /** True while one of our rockets is still boosting the player. */
  private boolean hasActiveRocket() {
    for (Entity entity : minecraft.level.entitiesForRendering()) {
      if (entity instanceof FireworkRocketEntity rocket && rocket.isAlive()) {
        if (rocket.distanceTo(minecraft.player) < 6.0f) {
          return true;
        }
      }
    }
    return false;
  }

  private void fire(int slot) {
    boolean needSwitch =
        autoSwitch.getValue() && slot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(slot);
    } else if (slot != minecraft.player.getInventory().getSelectedSlot()) {
      return;
    }

    minecraft
        .getConnection()
        .send(
            new net.minecraft.network.protocol.game.ServerboundUseItemPacket(
                InteractionHand.MAIN_HAND,
                0,
                minecraft.player.getYRot(),
                minecraft.player.getXRot()));

    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
    if (needSwitch && switchBack.getValue()) {
      PlayerUtil.swapBack();
    }
    DebugLogger.get()
        .log(
            getLabel(),
            DebugLogger.Level.INFO,
            "Fired firework from slot "
                + slot
                + " at "
                + minecraft.player.blockPosition().toShortString());
  }
}
