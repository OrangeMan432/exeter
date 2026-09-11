package me.larp.client.module.impl.toggle.movement;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;

/**
 * Baritone-lite: walks to target XZ with sprint, jumping, and arrival stop.
 * Set coordinates in ClickGui or via .goto (coming with the command pass).
 */
public class Goto extends ToggleableModule {

  private final NumberProperty<Double> targetX =
      new NumberProperty<Double>(0.0, -30000000.0, 30000000.0, "Target X");
  private final NumberProperty<Double> targetZ =
      new NumberProperty<Double>(0.0, -30000000.0, 30000000.0, "Target Z");
  private final NumberProperty<Double> arriveDist =
      new NumberProperty<Double>(2.0, 0.5, 10.0, "Arrive Dist");
  private final Property<Boolean> sprint =
      new Property<Boolean>(true, "Sprint");
  private final Property<Boolean> jump =
      new Property<Boolean>(true, "Jump");
  private final Property<Boolean> mine =
      new Property<Boolean>(true, "Mine");

  public Goto() {
    super("Goto", new String[] {"goto", "pathfind", "walk-to"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Walks to coordinates automatically.");
    offerProperties(targetX, targetZ, arriveDist, sprint, jump, mine);
    this.listeners.add(
        new Listener<TickEvent>("goto_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Goto.this.onTick();
          }
        });
  }

  public void setTarget(double x, double z) {
    targetX.setValue(x);
    targetZ.setValue(z);
    if (!isRunning()) {
      setRunning(true);
    }
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    double dx = targetX.getValue() - minecraft.player.getX();
    double dz = targetZ.getValue() - minecraft.player.getZ();
    double distSq = dx * dx + dz * dz;
    if (distSq <= arriveDist.getValue() * arriveDist.getValue()) {
      setRunning(false);
      return;
    }

    float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    minecraft.player.setYRot(yaw);
    minecraft.player.zza = 1.0f;
    if (sprint.getValue()) {
      minecraft.player.setSprinting(true);
    }
    if (jump.getValue()
        && minecraft.player.onGround()
        && minecraft.player.horizontalCollision) {
      minecraft.player.jumpFromGround();
    }
    // Baritone-native behavior: mine straight through soft walls.
    if (mine.getValue() && minecraft.player.horizontalCollision) {
      mineAhead();
    }
    setTag(String.format("Goto [%.0fm]", Math.sqrt(distSq)));
  }

  private void mineAhead() {
    double yawRad = Math.toRadians(minecraft.player.getYRot());
    net.minecraft.core.BlockPos ahead =
        minecraft.player.blockPosition().relative(
            net.minecraft.core.Direction.fromYRot(minecraft.player.getYRot()));
    for (net.minecraft.core.BlockPos pos :
        new net.minecraft.core.BlockPos[] {ahead, ahead.above()}) {
      net.minecraft.world.level.block.state.BlockState state =
          minecraft.level.getBlockState(pos);
      if (state.isAir()) continue;
      if (state.getBlock() == net.minecraft.world.level.block.Blocks.BEDROCK
          || state.getBlock() == net.minecraft.world.level.block.Blocks.OBSIDIAN) continue;
      swapToBestTool(state);
      net.minecraft.core.Direction face = faceOf(pos);
      minecraft.getConnection().send(
          new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
              net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action
                  .START_DESTROY_BLOCK,
              pos,
              face));
      minecraft.getConnection().send(
          new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
              net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action
                  .STOP_DESTROY_BLOCK,
              pos,
              face));
      return;
    }
  }

  private net.minecraft.core.Direction faceOf(net.minecraft.core.BlockPos pos) {
    for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
      if (!me.larp.client.util.PlayerUtil.isSolid(pos.relative(dir))) {
        return dir.getOpposite();
      }
    }
    return net.minecraft.core.Direction.UP;
  }

  private void swapToBestTool(net.minecraft.world.level.block.state.BlockState state) {
    int best = -1;
    float bestSpeed = 1.0f;
    for (int i = 0; i < 9; i++) {
      net.minecraft.world.item.ItemStack stack = minecraft.player.getInventory().getItem(i);
      if (stack.isEmpty()) continue;
      float speed = stack.getDestroySpeed(state);
      if (speed > bestSpeed) {
        bestSpeed = speed;
        best = i;
      }
    }
    if (best != -1 && best != minecraft.player.getInventory().getSelectedSlot()) {
      me.larp.client.util.PlayerUtil.swapTo(best);
    }
  }
}
