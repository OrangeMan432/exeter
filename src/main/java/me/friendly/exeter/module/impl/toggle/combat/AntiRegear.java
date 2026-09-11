package me.friendly.exeter.module.impl.toggle.combat;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lemon-pattern AntiRegear: breaks enemy shulkers and ender chests in range
 * so nobody restocks mid-fight. Packet mining with tool swap.
 */
public class AntiRegear extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Range");
  private final Property<Boolean> shulkers =
      new Property<Boolean>(true, "Shulkers");
  private final Property<Boolean> enderChests =
      new Property<Boolean>(true, "Ender Chests");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  public AntiRegear() {
    super("AntiRegear", new String[] {"antiregear", "anti-regear"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Breaks nearby shulkers and ender chests.");
    offerProperties(range, shulkers, enderChests, rotate, autoSwitch, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("antiregear_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AntiRegear.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    BlockPos target = findContainer();
    if (target == null) return;
    swapToBestTool(minecraft.level.getBlockState(target));

    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(target), PlayerUtil.getPitch(target));
    }
    Direction face = bestFace(target);
    minecraft.getConnection().send(
        new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, target, face));
    minecraft.getConnection().send(
        new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, target, face));
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    setTag("AntiRegear [" + minecraft.level.getBlockState(target).getBlock().getName().getString() + "]");
  }

  private BlockPos findContainer() {
    BlockPos origin = minecraft.player.blockPosition();
    int r = (int) Math.ceil(range.getValue());
    BlockPos best = null;
    double bestDist = Double.MAX_VALUE;
    for (int x = -r; x <= r; x++) {
      for (int y = -r; y <= r; y++) {
        for (int z = -r; z <= r; z++) {
          BlockPos pos = origin.offset(x, y, z);
          Block block = minecraft.level.getBlockState(pos).getBlock();
          boolean wanted =
              (shulkers.getValue() && block instanceof net.minecraft.world.level.block.ShulkerBoxBlock)
                  || (enderChests.getValue() && block == Blocks.ENDER_CHEST);
          if (!wanted) continue;
          if (!PlayerUtil.inRange(pos, range.getValue())) continue;
          double d = pos.distSqr(origin);
          if (d < bestDist) {
            bestDist = d;
            best = pos;
          }
        }
      }
    }
    return best;
  }

  private Direction bestFace(BlockPos pos) {
    for (Direction dir : Direction.values()) {
      if (!PlayerUtil.isSolid(pos.relative(dir))) {
        return dir.getOpposite();
      }
    }
    return Direction.UP;
  }

  private void swapToBestTool(BlockState state) {
    if (!autoSwitch.getValue()) return;
    int best = -1;
    float bestSpeed = 1.0f;
    for (int i = 0; i < 9; i++) {
      ItemStack stack = minecraft.player.getInventory().getItem(i);
      if (stack.isEmpty()) continue;
      float speed = stack.getDestroySpeed(state);
      if (speed > bestSpeed) {
        bestSpeed = speed;
        best = i;
      }
    }
    if (best != -1 && best != minecraft.player.getInventory().getSelectedSlot()) {
      PlayerUtil.swapTo(best);
    }
  }
}
