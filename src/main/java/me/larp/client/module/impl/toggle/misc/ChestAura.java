package me.larp.client.module.impl.toggle.misc;

import java.util.HashMap;
import java.util.Map;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Meteor-Rejects-pattern ChestAura: automatically opens storage blocks in
 * radius so ChestStealer can empty them. Remembers opened ones with a
 * forget timer.
 */
public class ChestAura extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(4.0, 1.0, 6.0, "Range");
  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(10, 0, 40, "Delay");
  private final NumberProperty<Integer> forgetAfter =
      new NumberProperty<Integer>(0, 0, 1000, "Forget After");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  private final Map<BlockPos, Integer> opened = new HashMap<>();
  private int tickCounter;
  private int lastOpenTick = -100;

  public ChestAura() {
    super("ChestAura", new String[] {"chestaura", "chest-aura"}, 0x00FFFF,
        ModuleType.MISCELLANEOUS);
    setDescription("Opens nearby storage for looting.");
    offerProperties(range, delay, forgetAfter, rotate, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("chestaura_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            ChestAura.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    opened.clear();
    tickCounter = 0;
    lastOpenTick = -100;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    tickCounter++;
    if (forgetAfter.getValue() > 0) {
      opened.entrySet().removeIf(e -> tickCounter - e.getValue() > forgetAfter.getValue());
    }
    if (tickCounter - lastOpenTick < delay.getValue()) return;

    BlockPos target = findStorage();
    if (target == null) return;

    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(target), PlayerUtil.getPitch(target));
    }
    PlayerUtil.useItemOn(target, Direction.UP);
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    opened.put(target.immutable(), tickCounter);
    // Double-chest compat: don't reopen the other half next tick.
    BlockState openedState = minecraft.level.getBlockState(target);
    if (openedState.hasProperty(ChestBlock.TYPE)) {
      var type = openedState.getValue(ChestBlock.TYPE);
      Direction facing = openedState.getValue(ChestBlock.FACING);
      switch (type) {
        case LEFT -> opened.put(target.relative(facing.getClockWise()).immutable(), tickCounter);
        case RIGHT ->
            opened.put(target.relative(facing.getCounterClockWise()).immutable(), tickCounter);
        default -> {}
      }
    }
    lastOpenTick = tickCounter;
  }

  private BlockPos findStorage() {
    BlockPos origin = minecraft.player.blockPosition();
    int r = (int) Math.ceil(range.getValue());
    BlockPos best = null;
    double bestDist = Double.MAX_VALUE;
    for (int x = -r; x <= r; x++) {
      for (int y = -r; y <= r; y++) {
        for (int z = -r; z <= r; z++) {
          BlockPos pos = origin.offset(x, y, z);
          if (opened.containsKey(pos)) continue;
          if (!isStorage(minecraft.level.getBlockState(pos))) continue;
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

  private static boolean isStorage(BlockState state) {
    var block = state.getBlock();
    return block == Blocks.CHEST
        || block == Blocks.TRAPPED_CHEST
        || block == Blocks.BARREL
        || block == Blocks.ENDER_CHEST
        || block == Blocks.SHULKER_BOX
        || block instanceof net.minecraft.world.level.block.ShulkerBoxBlock;
  }
}
