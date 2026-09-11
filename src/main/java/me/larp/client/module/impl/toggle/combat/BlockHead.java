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

/**
 * Lemon-pattern BlockHead: seats obsidian on multiple heads per tick to
 * blind and trap several targets at once.
 */
public class BlockHead extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(6.0, 1.0, 10.0, "Range");
  private final NumberProperty<Double> placeRange =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Place Range");
  private final NumberProperty<Integer> maxTargets =
      new NumberProperty<Integer>(3, 1, 8, "Max Targets");
  private final NumberProperty<Integer> blocksPerTick =
      new NumberProperty<Integer>(2, 1, 8, "Blocks Per Tick");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  public BlockHead() {
    super("BlockHead", new String[] {"blockhead", "head-fill"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Obsidian over several heads per tick.");
    offerProperties(range, placeRange, maxTargets, blocksPerTick, rotate, autoSwitch, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("blockhead_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            BlockHead.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    List<Player> targets = findTargets();
    if (targets.isEmpty()) return;
    int obbySlot = PlayerUtil.findInHotbar(BlockHead::isObsidian);
    if (obbySlot == -1) return;

    int placed = 0;
    for (Player target : targets) {
      if (placed >= blocksPerTick.getValue()) break;
      BlockPos head = target.blockPosition().above();
      if (!PlayerUtil.isAirOrReplaceable(head)) continue;
      if (!PlayerUtil.inRange(head, placeRange.getValue())) continue;
      placeObby(head, obbySlot);
      placed++;
    }
  }

  private List<Player> findTargets() {
    List<Player> all = new ArrayList<>();
    for (Entity entity : minecraft.level.players()) {
      if (entity == minecraft.player || !entity.isAlive()) continue;
      Player player = (Player) entity;
      if (minecraft.player.distanceTo(entity) > range.getValue()) continue;
      if (me.larp.client.core.Larp.getInstance()
          .getFriendManager()
          .isFriend(player.getName().getString())) continue;
      all.add(player);
    }
    all.sort(Comparator.comparingDouble(minecraft.player::distanceTo));
    return all.subList(0, Math.min(maxTargets.getValue(), all.size()));
  }

  private void placeObby(BlockPos pos, int slot) {
    boolean swapped = PlayerUtil.swapToSlot(slot, autoSwitch.getValue());
    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(pos), PlayerUtil.getPitch(pos));
    }
    PlayerUtil.clickNeighbor(pos);
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    PlayerUtil.swapBackIf(swapped);
  }

  private static boolean isObsidian(ItemStack stack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return false;
    return item.getBlock() == Blocks.OBSIDIAN;
  }
}
