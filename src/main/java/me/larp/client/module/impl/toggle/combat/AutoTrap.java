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
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Anarchy staple: cages the target in obsidian (sides + head + top). */
public class AutoTrap extends ToggleableModule {

  private final NumberProperty<Double> targetRange =
      new NumberProperty<Double>(8.0, 1.0, 12.0, "Target Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Place Range");
  private final NumberProperty<Integer> blocksPerTick =
      new NumberProperty<Integer>(2, 1, 8, "Blocks Per Tick");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> top = new Property<Boolean>(true, "Top");

  public AutoTrap() {
    super("AutoTrap", new String[] {"autotrap", "auto-trap"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Traps targets in obsidian.");
    offerProperties(targetRange, placeRange, blocksPerTick, rotate, autoSwitch, top);
    this.listeners.add(
        new Listener<TickEvent>("autotrap_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoTrap.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    Player target = findTarget();
    if (target == null) return;
    int obbySlot = PlayerUtil.findInHotbar(AutoTrap::isObsidian);
    if (obbySlot == -1) return;

    BlockPos feet = target.blockPosition();
    List<BlockPos> cage = new ArrayList<>();
    cage.add(feet.north());
    cage.add(feet.south());
    cage.add(feet.east());
    cage.add(feet.west());
    BlockPos head = feet.above();
    cage.add(head.north());
    cage.add(head.south());
    cage.add(head.east());
    cage.add(head.west());
    if (top.getValue()) {
      cage.add(head.above());
    }
    cage.sort(Comparator.comparingDouble(p -> p.distSqr(minecraft.player.blockPosition())));

    int placed = 0;
    for (BlockPos pos : cage) {
      if (placed >= blocksPerTick.getValue()) break;
      if (!PlayerUtil.isAirOrReplaceable(pos)) continue;
      if (!PlayerUtil.inRange(pos, placeRange.getValue())) continue;
      placeObby(pos, obbySlot);
      placed++;
    }
  }

  private Player findTarget() {
    Player best = null;
    double bestDist = targetRange.getValue();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player || !entity.isAlive()) continue;
      double d = minecraft.player.distanceTo(entity);
      if (d < bestDist) {
        bestDist = d;
        best = (Player) entity;
      }
    }
    return best;
  }

  private void placeObby(BlockPos pos, int slot) {
    boolean needSwitch =
        autoSwitch.getValue() && slot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(slot);
    }
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(pos), PlayerUtil.getPitch(pos));
    }
    PlayerUtil.useItemOn(pos.below(), Direction.UP);
    PlayerUtil.swingHand();
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    if (needSwitch) {
      PlayerUtil.swapBack();
    }
  }

  private static boolean isObsidian(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == Blocks.OBSIDIAN;
  }
}
