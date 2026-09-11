package me.friendly.exeter.module.impl.toggle.world;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Lemon-pattern SpeedMine: targeted packet mining with auto pickaxe swap and
 * per-tick START refresh so server progress never resets.
 */
public class SpeedMine extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(5.0, 1.0, 6.0, "Range");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");

  public SpeedMine() {
    super("SpeedMine", new String[] {"speedmine", "speed-mine"}, 0x0000FF, ModuleType.WORLD);
    setDescription("Mines the targeted block at packet speed.");
    offerProperties(range, rotate, autoSwitch, swingHand);
    this.listeners.add(
        new Listener<TickEvent>("speedmine_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            SpeedMine.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (!minecraft.gameMode.isDestroying()) return;
    if (!(minecraft.hitResult instanceof BlockHitResult hit)) return;

    BlockPos pos = hit.getBlockPos();
    BlockState state = minecraft.level.getBlockState(pos);
    if (state.isAir()) return;
    if (state.getBlock() == Blocks.BEDROCK
        || state.getBlock() == Blocks.REINFORCED_DEEPSLATE
        || state.getBlock() == Blocks.END_PORTAL_FRAME) {
      return;
    }
    if (!PlayerUtil.inRange(pos, range.getValue())) return;

    swapToBestTool(state);

    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      PlayerUtil.setRotation(PlayerUtil.getYaw(pos), PlayerUtil.getPitch(pos));
    }
    minecraft.getConnection().send(
        new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, hit.getDirection()));
    minecraft.getConnection().send(
        new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, hit.getDirection()));
    if (swingHand.getValue()) {
      PlayerUtil.swingHand();
    }
    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
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
