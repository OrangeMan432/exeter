package me.friendly.exeter.module.impl.toggle.world;

import java.util.Set;
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

/** Automatically mines ores and valuables in range with packet speed. */
public class AutoMine extends ToggleableModule {

  private static final Set<Block> ORES = Set.of(
      Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
      Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
      Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
      Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
      Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
      Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
      Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
      Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE,
      Blocks.ANCIENT_DEBRIS);

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Range");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  public AutoMine() {
    super("AutoMine", new String[] {"automine", "auto-mine"}, 0x0000FF, ModuleType.WORLD);
    setDescription("Mines ores around you automatically.");
    offerProperties(range, rotate, autoSwitch, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("automine_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoMine.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    BlockPos target = findOre();
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
    setTag("AutoMine [" + minecraft.level.getBlockState(target).getBlock().getName().getString() + "]");
  }

  private BlockPos findOre() {
    BlockPos origin = minecraft.player.blockPosition();
    int r = (int) Math.ceil(range.getValue());
    BlockPos best = null;
    double bestDist = Double.MAX_VALUE;
    for (int x = -r; x <= r; x++) {
      for (int y = -r; y <= r; y++) {
        for (int z = -r; z <= r; z++) {
          BlockPos pos = origin.offset(x, y, z);
          if (!ORES.contains(minecraft.level.getBlockState(pos).getBlock())) continue;
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
