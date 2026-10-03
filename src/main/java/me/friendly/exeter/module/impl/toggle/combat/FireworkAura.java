package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.logging.DebugLogger;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;

/**
 * Launches damaging fireworks at the feet of players fighting under a roof. Only rockets carrying
 * an explosion charge are used; blanks are skipped via NBT. Shorter flight durations are preferred
 * so the burst stays on the target.
 */
public class FireworkAura extends ToggleableModule {

  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(6.0, 0.0, 12.0, "Target Range");
  private final NumberProperty<Integer> delay = new NumberProperty<Integer>(10, 0, 40, "Delay");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> switchBack = new Property<Boolean>(true, "Switch Back");

  private int tickCounter;
  private int lastFireTick;
  private String lastTargetKey;
  private boolean noRocketLogged;

  public FireworkAura() {
    super(
        "FireworkAura",
        new String[] {"fireworkaura", "firework-aura"},
        0xFFAA55,
        ModuleType.COMBAT);
    setDescription("Fireworks players hiding under cover.");
    offerProperties(targetRange, delay, rotate, swingHand, autoSwitch, switchBack);
    this.listeners.add(
        new Listener<TickEvent>("fireworkaura_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });
    this.listeners.add(
        new Listener<PacketEvent>("fireworkaura_packet") {
          @Override
          public void call(PacketEvent event) {
            PlayerUtil.spoofMovement(event);
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    lastTargetKey = null;
    noRocketLogged = false;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "enabled");
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    lastTargetKey = null;
    noRocketLogged = false;
    DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "disabled");
  }

  private void onTick() {
    tickCounter++;
    if (minecraft.player == null || minecraft.level == null) return;
    if (tickCounter - lastFireTick < delay.getValue()) return;

    Player target = findTarget();
    if (target == null) {
      lastTargetKey = null;
      return;
    }
    String targetKey = target.getName().getString();
    if (!targetKey.equals(lastTargetKey)) {
      lastTargetKey = targetKey;
      noRocketLogged = false;
      DebugLogger.get().log(getLabel(), DebugLogger.Level.INFO, "target=" + targetKey);
    }
    int rocketSlot = findRocketSlot();
    if (rocketSlot == -1) {
      if (!noRocketLogged) {
        noRocketLogged = true;
        DebugLogger.get().log(getLabel(), DebugLogger.Level.WARN, "no damaging rockets in hotbar");
      }
      return;
    }
    noRocketLogged = false;

    BlockPos ground = target.blockPosition().below();
    if (!PlayerUtil.inRange(ground, targetRange.getValue() + 2.0)) return;

    int origSlot = minecraft.player.getInventory().getSelectedSlot();
    boolean needSwitch =
        autoSwitch.getValue() && rocketSlot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(rocketSlot);
      minecraft.player.getInventory().setSelectedSlot(rocketSlot);
    }
    PlayerUtil.sendSpoofTopUp();
    Runnable fire =
        () -> {
          PlayerUtil.useItemOn(ground, Direction.UP);
          if (swingHand.getValue()) PlayerUtil.swingHand();
          if (needSwitch && switchBack.getValue()) PlayerUtil.swapBack();
        };
    if (rotate.getValue()) {
      PlayerUtil.withRotation(
          (float) PlayerUtil.getYaw(ground), (float) PlayerUtil.getPitch(ground), fire);
    } else {
      fire.run();
    }
    if (autoSwitch.getValue() && switchBack.getValue()) {
      minecraft.player.getInventory().setSelectedSlot(origSlot);
    }
    PlayerUtil.resyncSlot();
    DebugLogger.get()
        .logFile(
            getLabel(),
            "fire slot="
                + rocketSlot
                + " at "
                + ground.toShortString()
                + " target="
                + target.getName().getString());
    lastFireTick = tickCounter;
  }

  private int findRocketSlot() {
    int best = -1;
    int bestDuration = Integer.MAX_VALUE;
    for (int i = 0; i < 9; i++) {
      var stack = minecraft.player.getInventory().getItem(i);
      if (stack.isEmpty() || !stack.is(Items.FIREWORK_ROCKET)) continue;
      Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
      if (fireworks == null || fireworks.explosions().isEmpty()) continue;
      if (fireworks.flightDuration() < bestDuration) {
        bestDuration = fireworks.flightDuration();
        best = i;
      }
    }
    return best;
  }

  private Player findTarget() {
    List<Player> players = new ArrayList<>();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player) continue;
      if (!entity.isAlive()) continue;
      if (!(entity instanceof Player)) continue;
      if (!Exeter.getInstance().getFriendManager().isTargetable(entity.getName().getString())) {
        continue;
      }
      double distance = minecraft.player.distanceTo(entity);
      if (distance > targetRange.getValue()) continue;
      // Needs a roof: solid block two above the feet so the burst is contained.
      BlockPos aboveHead = entity.blockPosition().above(2);
      var state = minecraft.level.getBlockState(aboveHead);
      if (state.isAir() || state.canBeReplaced()) continue;
      players.add((Player) entity);
    }
    return players.stream()
        .min(Comparator.comparingDouble(p -> minecraft.player.distanceTo(p)))
        .orElse(null);
  }
}
