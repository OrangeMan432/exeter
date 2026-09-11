package me.larp.client.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Lemon-pattern AutoAnvil: drops anvils onto targets from above.
 * Anvils fall, so only air clearance above the head is needed.
 */
public class AutoAnvil extends ToggleableModule {

  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(8.0, 1.0, 12.0, "Target Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Place Range");
  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(4, 0, 20, "Delay");
  private final NumberProperty<Integer> height =
      new NumberProperty<Integer>(3, 2, 6, "Height");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  private int tickCounter;
  private int lastPlaceTick = -100;

  public AutoAnvil() {
    super("AutoAnvil", new String[] {"autoanvil", "anvil"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Drops anvils on targets.");
    offerProperties(targetRange, placeRange, delay, height, rotate, autoSwitch, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("autoanvil_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoAnvil.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = 0;
    lastPlaceTick = -100;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    tickCounter++;
    if (tickCounter - lastPlaceTick < delay.getValue()) return;

    Player target = findTarget();
    if (target == null) return;
    int anvilSlot = PlayerUtil.findInHotbar(AutoAnvil::isAnvil);
    if (anvilSlot == -1) return;

    BlockPos drop = target.blockPosition().above(height.getValue());
    if (!PlayerUtil.isAirOrReplaceable(drop)) return;
    if (!PlayerUtil.inRange(drop, placeRange.getValue())) return;

    boolean swapped = PlayerUtil.swapToSlot(anvilSlot, autoSwitch.getValue());
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(drop), PlayerUtil.getPitch(drop));
    }
    PlayerUtil.clickNeighbor(drop);
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    PlayerUtil.swapBackIf(swapped);
    lastPlaceTick = tickCounter;
  }

  private Player findTarget() {
    Player best = null;
    double bestDist = targetRange.getValue();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player || !entity.isAlive()) continue;
      Player player = (Player) entity;
      if (me.larp.client.core.Larp.getInstance()
          .getFriendManager()
          .isFriend(player.getName().getString())) continue;
      double d = minecraft.player.distanceTo(entity);
      if (d < bestDist) {
        bestDist = d;
        best = player;
      }
    }
    return best;
  }

  private static boolean isAnvil(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == Blocks.ANVIL
        || item.getBlock() == Blocks.CHIPPED_ANVIL
        || item.getBlock() == Blocks.DAMAGED_ANVIL;
  }
}
