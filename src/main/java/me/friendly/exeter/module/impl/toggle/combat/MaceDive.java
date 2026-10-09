package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.util.MathUtil;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.phys.Vec3;

/**
 * Elytra dive bomber: climbs above the nearest enemy with firework boosts, dives onto them, swaps
 * the elytra for a chestplate at the last second, and lands a mace smash.
 */
public class MaceDive extends ToggleableModule {

  private enum Phase {
    SEEK,
    DIVE
  }

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(40.0, 5.0, 120.0, "Range", "range");
  private final NumberProperty<Double> climb =
      new NumberProperty<Double>(15.0, 5.0, 40.0, "Climb Height", "climb");
  private final NumberProperty<Double> strikeRange =
      new NumberProperty<Double>(3.0, 2.0, 8.0, "Strike Range", "strikerange");
  private final NumberProperty<Double> swapRange =
      new NumberProperty<Double>(20.0, 20.0, 40.0, "Swap Range", "swaprange");
  private final NumberProperty<Integer> cooldown =
      new NumberProperty<Integer>(12, 0, 200, "Strike Cooldown", "cooldown");
  private final NumberProperty<Float> turnSpeed =
      new NumberProperty<Float>(30.0f, 1.0f, 180.0f, "Turn Speed", "turnspeed");

  private Player target;
  private Phase phase = Phase.SEEK;
  private int cooldownTicks;
  private int flyTicks;
  private int ticksSinceFlight;
  private boolean pullingUp;
  private boolean strikeBoostPending;
  private boolean committed;
  private boolean swingLogged;
  private int recoverTicks;
  private boolean airNotified;
  private boolean maceHeld;
  private boolean rocketsWarned;
  private boolean chestWarned;
  private boolean elytraWarned;
  private float aimYaw;
  private float aimPitch;
  private boolean aimInit;

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("mace_dive_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          MaceDive.this.onTick();
        }
      };

  public MaceDive() {
    super(
        "MaceDive",
        new String[] {"macedive", "mace-dive", "divebomber"},
        0xFFAA00,
        ModuleType.COMBAT);
    setDescription("Flies above enemies, dives, swaps to chestplate and mace smashes.");
    offerProperties(range, climb, strikeRange, swapRange, cooldown, turnSpeed);
    range.setDescription("Hunt enemies within this many blocks.");
    climb.setDescription("Height above the target to reach before diving.");
    strikeRange.setDescription("Swing the mace within this many blocks of the target.");
    swapRange.setDescription("Distance at which the elytra swaps to a chestplate.");
    cooldown.setDescription("Ticks between mace swings.");
    turnSpeed.setDescription("Degrees steered per tick while flying.");
    this.listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    target = null;
    phase = Phase.SEEK;
    cooldownTicks = 0;
    flyTicks = 0;
    ticksSinceFlight = 0;
    committed = false;
    rocketsWarned = false;
    chestWarned = false;
    elytraWarned = false;
    airNotified = false;
    aimInit = false;
    DebugLogger.get()
        .log(
            getLabel(),
            DebugLogger.Level.INFO,
            "Enabled (range=" + range.getValue() + ", climb=" + climb.getValue() + ")");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    target = null;
    aimInit = false;
    committed = false;
    recoverTicks = 0;
    strikeBoostPending = false;
    releaseMace();
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.player.isDeadOrDying()) {
      return;
    }
    if (!minecraft.player.isFallFlying()) {
      ticksSinceFlight++;
      if (committed && !minecraft.player.onGround()) {
        // Committed final fall: hands off until impact, strikes are evaluated below.
        flyTicks = 0;
      } else {
        committed = false;
        // Landed: the previous dive is over, start the next run from SEEK so rockets
        // and climb steering re-engage instead of idling in a stale DIVE.
        phase = Phase.SEEK;
        if (!airNotified) {
          airNotified = true;
          DebugLogger.get()
              .log(getLabel(), DebugLogger.Level.WARN, "Take off with an elytra first");
        }
        // Keep a takeoff-ready elytra equipped while grounded so the run can start.
        wearElytra();
        if (!isElytra(minecraft.player.getItemBySlot(EquipmentSlot.CHEST))
            && findContainerArmor(false) == -1) {
          if (!elytraWarned) {
            elytraWarned = true;
            DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "No elytra in inventory");
          }
        } else {
          elytraWarned = false;
        }
        if (target != null
            && ticksSinceFlight < 100
            && minecraft.player.onGround()
            && isElytra(minecraft.player.getItemBySlot(EquipmentSlot.CHEST))) {
          // Relaunch with a sprint-jump: deploying with zero airspeed stalls out
          // instantly (redeploy/land flap). Sprint is released once flight is set.
          minecraft.player.setSprinting(true);
          minecraft.player.jumpFromGround();
        }
        if (target != null
            && !minecraft.player.onGround()
            && minecraft.player.fallDistance > 0.0f
            && isElytra(minecraft.player.getItemBySlot(EquipmentSlot.CHEST))
            && minecraft.player.tryToStartFallFlying()) {
          // Fell off after a strike with a target: redeploy directly instead of pulsing
          // jump input, which latches state vanilla never clears and jams manual takeoff.
          minecraft
              .getConnection()
              .send(
                  new ServerboundPlayerCommandPacket(
                      minecraft.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
          DebugLogger.get().logFile(getLabel(), "Redeployed elytra");
        }
      }
      flyTicks = 0;
    } else {
      airNotified = false;
      flyTicks++;
      ticksSinceFlight = 0;
      if (flyTicks > 30) {
        minecraft.player.setSprinting(false);
      }
    }
    if (cooldownTicks > 0) {
      cooldownTicks--;
    }

    Player next = findTarget();
    if (next != target) {
      target = next;
      aimInit = false;
      strikeBoostPending = false;
      committed = false;
      swingLogged = false;
      recoverTicks = 0;
      releaseMace();
      if (target != null) {
        phase = Phase.SEEK;
        DebugLogger.get().logFile(getLabel(), "Target acquired: " + describe());
      }
    }
    if (target == null || !target.isAlive()) {
      releaseMace();
      return;
    }

    // Strikes only land smashes while truly falling: gliding flybys are left to the dive
    // window, which sheds the elytra first. Swings are strength-gated so every attempt
    // is a full-strength hit instead of spam. The lead term compensates server-side
    // position staleness: at dive speed the server is up to a tick behind us, so its
    // picture of us is farther out and unadjusted swings always whiff.
    double preDist = minecraft.player.distanceTo(target);
    double lead = minecraft.player.getDeltaMovement().length();
    if (cooldownTicks <= 0
        && preDist + lead <= strikeRange.getValue()
        && (!minecraft.player.isFallFlying() || committed)
        && minecraft.player.getAttackStrengthScale(0.5f) > 0.9f) {
      strike();
      return;
    }
    if (!minecraft.player.isFallFlying()) {
      return;
    }
    if (recoverTicks > 0) {
      // Climbing out of a strike: straight up with rocket power when available.
      recoverTicks--;
      if (!hasActiveRocket()) {
        fireRocket();
      }
      steerAt(minecraft.player.getX(), minecraft.player.getY() + 15.0, minecraft.player.getZ());
      return;
    }

    double horizontal =
        Math.sqrt(
            Math.pow(target.getX() - minecraft.player.getX(), 2)
                + Math.pow(target.getZ() - minecraft.player.getZ(), 2));
    double heightAbove = minecraft.player.getY() - target.getY();
    if (phase == Phase.SEEK) {
      wearElytra();
      int rocketSlot =
          PlayerUtil.findInHotbar(stack -> !stack.isEmpty() && stack.is(Items.FIREWORK_ROCKET));
      if (rocketSlot == -1) {
        if (!rocketsWarned) {
          rocketsWarned = true;
          DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "No fireworks in hotbar");
        }
      } else {
        rocketsWarned = false;
      }
      // Boost only while slow so approaches stay trackable, plus one recovery boost right
      // after a strike to climb out of the dive.
      if (!hasActiveRocket()
          && (strikeBoostPending || minecraft.player.getDeltaMovement().length() < 0.8)) {
        fireRocket();
        strikeBoostPending = false;
      }
      // Dive once established in flight and positioned above: the tick gate stops instant
      // dives at takeoff. Entry is deliberately early so the descent is a shallow glide
      // onto the target instead of a last-second lawn-dart that overshoots.
      if (flyTicks > 30 && horizontal < 12.0 && heightAbove >= 8.0) {
        phase = Phase.DIVE;
        DebugLogger.get().logFile(getLabel(), "DIVE: " + describe());
      }
      steerAt(target.getX(), target.getY() + climb.getValue(), target.getZ());
    } else {
      // Dive with the elytra ON: removing it kills gliding and turns the dive into an
      // uncontrolled fall. The swap happens once in the final window (see below), then
      // recovery stays off until the swing lands so the module stops fighting itself.
      double dist = minecraft.player.distanceTo(target);
      if (!committed && flyTicks > 30 && dist <= swapRange.getValue()) {
        if (wearChestplate()) {
          chestWarned = false;
          holdMace();
          committed = true;
          swingLogged = false;
          DebugLogger.get().logFile(getLabel(), "Committed: " + describe());
        } else if (!chestWarned) {
          chestWarned = true;
          DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "No chestplate to swap to");
        }
      }
      if (!committed && heightAbove < 6.0 && horizontal < 6.0) {
        // Low uncommitted pass with no strike lined up: pull up instead of lawn-darting.
        // Committed runs are left alone to conclude (strike or miss).
        if (!pullingUp) {
          pullingUp = true;
          DebugLogger.get().logFile(getLabel(), "Pull-up: " + describe());
        }
        steerAt(target.getX(), minecraft.player.getY() + 12.0, target.getZ());
      } else {
        pullingUp = false;
        steerAt(target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
      }
      if (horizontal > 12.0) {
        phase = Phase.SEEK;
        committed = false;
        releaseMace();
        strikeBoostPending = true;
      }
    }
  }

  private void strike() {
    if (!maceHeld && !holdMace()) {
      DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "No mace in hotbar");
      cooldownTicks = cooldown.getValue();
      return;
    }
    minecraft.gameMode.attack(minecraft.player, target);
    PlayerUtil.swingHand();
    cooldownTicks = cooldown.getValue();
    // Re-arm the elytra at once and climb out: without immediate recovery the diver
    // splats into the ground it just dove at and pops its own totem.
    wearElytra();
    recoverTicks = 25;
    // The pass continues (phase/commit untouched) so tracking failures get more attempts
    // instead of one snapshot swing; elytra re-arm happens on abort via SEEK.
    DebugLogger.get().logFile(getLabel(), "Strike: " + describe());
    if (!swingLogged) {
      swingLogged = true;
      DebugLogger.get()
          .log(
              getLabel(),
              DebugLogger.Level.INFO,
              "Mace smash on "
                  + target.getName().getString()
                  + " from "
                  + String.format("%.1f", minecraft.player.distanceTo(target))
                  + "m");
    }
  }

  private void steerAt(double x, double y, double z) {
    Vec3 eye = minecraft.player.getEyePosition();
    double dx = x - eye.x;
    double dy = y - eye.y;
    double dz = z - eye.z;
    float wantYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    if (!aimInit) {
      aimYaw = minecraft.player.getYRot();
      aimPitch = minecraft.player.getXRot();
      aimInit = true;
    }
    float step = turnSpeed.getValue();
    aimYaw += MathUtil.clamp(Mth.wrapDegrees(wantYaw - aimYaw), -step, step);
    aimPitch += MathUtil.clamp(wantPitch - aimPitch, -step, step);
    aimYaw = Mth.wrapDegrees(aimYaw);
    aimPitch = MathUtil.clamp(aimPitch, -90.0f, 90.0f);
    PlayerUtil.setRotation(aimYaw, aimPitch);
  }

  private String describe() {
    if (minecraft.player == null || target == null) {
      return "no target";
    }
    Vec3 me = minecraft.player.position();
    Vec3 you = target.position();
    double horizontal = Math.sqrt(Math.pow(you.x - me.x, 2) + Math.pow(you.z - me.z, 2));
    return String.format(
        "me=%s@(%.1f, %.1f, %.1f) target=%s@(%.1f, %.1f, %.1f) dist=%.1f horiz=%.1f above=%.1f"
            + " flying=%s phase=%s",
        minecraft.player.getName().getString(),
        me.x,
        me.y,
        me.z,
        target.getName().getString(),
        you.x,
        you.y,
        you.z,
        (float) minecraft.player.distanceTo(target),
        (float) horizontal,
        (float) (me.y - you.y),
        minecraft.player.isFallFlying(),
        phase);
  }

  private Player findTarget() {
    List<Player> enemies = new ArrayList<>();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player) continue;
      if (!entity.isAlive()) continue;
      if (!Exeter.getInstance().getFriendManager().isTargetable(entity.getName().getString())) {
        continue;
      }
      if (minecraft.player.distanceTo(entity) <= range.getValue()) {
        enemies.add((Player) entity);
      }
    }
    return enemies.stream()
        .min(Comparator.comparingDouble(minecraft.player::distanceTo))
        .orElse(null);
  }

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

  private void fireRocket() {
    int slot =
        PlayerUtil.findInHotbar(stack -> !stack.isEmpty() && stack.is(Items.FIREWORK_ROCKET));
    if (slot == -1) {
      return;
    }
    boolean needSwitch = slot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(slot);
    }
    minecraft
        .getConnection()
        .send(
            new net.minecraft.network.protocol.game.ServerboundUseItemPacket(
                InteractionHand.MAIN_HAND,
                0,
                minecraft.player.getYRot(),
                minecraft.player.getXRot()));
    PlayerUtil.swingHand();
    if (needSwitch) {
      PlayerUtil.swapBack();
    }
  }

  /** Holds the mace ahead of the strike so the server sees it in hand on impact. */
  private boolean holdMace() {
    int maceSlot = findMaceSlot();
    if (maceSlot == -1) {
      return false;
    }
    if (maceSlot != minecraft.player.getInventory().getSelectedSlot()) {
      PlayerUtil.swapTo(maceSlot);
    }
    maceHeld = true;
    return true;
  }

  /**
   * Finds the mace anywhere, pulling it from the main inventory into the hotbar when needed so a
   * rearranged bar can never silently disarm the strike.
   */
  private int findMaceSlot() {
    int hotbar = PlayerUtil.findInHotbar(stack -> !stack.isEmpty() && stack.is(Items.MACE));
    if (hotbar != -1) {
      return hotbar;
    }
    int containerId = minecraft.player.containerMenu.containerId;
    for (int i = 9; i < 36; i++) {
      ItemStack stack = minecraft.player.containerMenu.getSlot(i).getItem();
      if (!stack.isEmpty() && stack.is(Items.MACE)) {
        int hotbarIndex = firstEmptyHotbar();
        if (hotbarIndex == -1) {
          hotbarIndex = minecraft.player.getInventory().getSelectedSlot();
        }
        minecraft.gameMode.handleContainerInput(
            containerId, i, hotbarIndex, ContainerInput.SWAP, minecraft.player);
        DebugLogger.get().logFile(getLabel(), "Pulled mace into hotbar slot " + hotbarIndex);
        return hotbarIndex;
      }
    }
    return -1;
  }

  private int firstEmptyHotbar() {
    for (int i = 0; i < 9; i++) {
      if (minecraft.player.getInventory().getItem(i).isEmpty()) {
        return i;
      }
    }
    return -1;
  }

  private void releaseMace() {
    if (maceHeld) {
      PlayerUtil.swapBack();
      maceHeld = false;
    }
  }

  private boolean isElytra(ItemStack stack) {
    return !stack.isEmpty() && stack.is(Items.ELYTRA);
  }

  private boolean isChestplate(ItemStack stack) {
    if (stack.isEmpty() || stack.is(Items.ELYTRA)) {
      return false;
    }
    Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
    return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
  }

  private static final int CHEST_ARMOR_SLOT = 6;

  private boolean wearChestplate() {
    if (isChestplate(minecraft.player.getItemBySlot(EquipmentSlot.CHEST))) {
      return true;
    }
    int slot = findContainerArmor(true);
    if (slot == -1) {
      return false;
    }
    swapIntoChest(slot);
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "Swapped elytra for chestplate");
    return true;
  }

  private void wearElytra() {
    if (isElytra(minecraft.player.getItemBySlot(EquipmentSlot.CHEST))) {
      return;
    }
    int slot = findContainerArmor(false);
    if (slot == -1) {
      return;
    }
    swapIntoChest(slot);
    DebugLogger.get().logFile(getLabel(), "Re-equipped elytra");
  }

  /**
   * Explicit pickup swap into the chest armor slot. Deterministic unlike shift-click, which the
   * server did not always honor mid-dive.
   */
  private void swapIntoChest(int sourceContainerSlot) {
    int containerId = minecraft.player.containerMenu.containerId;
    minecraft.gameMode.handleContainerInput(
        containerId, sourceContainerSlot, 0, ContainerInput.PICKUP, minecraft.player);
    minecraft.gameMode.handleContainerInput(
        containerId, CHEST_ARMOR_SLOT, 0, ContainerInput.PICKUP, minecraft.player);
    minecraft.gameMode.handleContainerInput(
        containerId, sourceContainerSlot, 0, ContainerInput.PICKUP, minecraft.player);
  }

  /** Finds a chest armor piece (or elytra) in the main inventory container slots. */
  private int findContainerArmor(boolean chestplate) {
    for (int i = 9; i < 45; i++) {
      ItemStack stack = minecraft.player.containerMenu.getSlot(i).getItem();
      if (chestplate ? isChestplate(stack) : isElytra(stack)) {
        return i;
      }
    }
    return -1;
  }
}
