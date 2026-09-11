package me.larp.client.module.impl.toggle.world;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.RenderWorldEvent;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.util.PlayerUtil;
import me.larp.client.util.Render3D;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Meteor-Rejects-pattern Lavacast: measures the drop, then cycles
 * lava-down, pickup, water-down, pickup to stack a cobble pillar.
 */
public class Lavacast extends ToggleableModule {

  private enum Phase {
    NONE,
    LAVA_DOWN,
    LAVA_UP,
    WATER_DOWN,
    WATER_UP
  }

  private final NumberProperty<Integer> tickInterval =
      new NumberProperty<Integer>(2, 0, 20, "Tick Interval");
  private final NumberProperty<Integer> distMin =
      new NumberProperty<Integer>(5, 0, 10, "Min Distance");
  private final NumberProperty<Integer> lavaDownMult =
      new NumberProperty<Integer>(40, 1, 100, "Lava Down Mult");
  private final NumberProperty<Integer> lavaUpMult =
      new NumberProperty<Integer>(8, 1, 100, "Lava Up Mult");
  private final NumberProperty<Integer> waterDownMult =
      new NumberProperty<Integer>(4, 1, 100, "Water Down Mult");
  private final NumberProperty<Integer> waterUpMult =
      new NumberProperty<Integer>(1, 1, 100, "Water Up Mult");

  private int dist;
  private BlockPos placePos;
  private int tick;
  private Phase phase = Phase.NONE;

  public Lavacast() {
    super("Lavacast", new String[] {"lavacast", "lava-cast"}, 0x0000FF, ModuleType.WORLD);
    setDescription("Automatic lava casting machine.");
    offerProperties(
        tickInterval, distMin, lavaDownMult, lavaUpMult, waterDownMult, waterUpMult);
    this.listeners.add(
        new Listener<TickEvent>("lavacast_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Lavacast.this.onTick();
          }
        });
    this.listeners.add(
        new Listener<RenderWorldEvent>("lavacast_render") {
          @Override
          public void call(RenderWorldEvent event) {
            Lavacast.this.onRender(event);
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tick = 0;
    phase = Phase.NONE;
    if (minecraft.player == null || minecraft.level == null) {
      setRunning(false);
      return;
    }
    placePos = targetBlock();
    if (placePos == null) {
      placePos = minecraft.player.blockPosition().below(2);
    } else {
      placePos = placePos.above();
    }
    dist = -1;
    measure(new Vec3i(1, 0, 0));
    measure(new Vec3i(-1, 0, 0));
    measure(new Vec3i(0, 0, 1));
    measure(new Vec3i(0, 0, -1));
    if (dist < 1) {
      setRunning(false);
      return;
    }
    setTag("Lavacast [" + dist + "]");
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying() || placePos == null) return;
    tick++;
    if (waiting()) return;
    if (dist < distMin.getValue()) {
      setRunning(false);
      return;
    }
    tick = 0;
    // Mine through anything occupying the pour spot first.
    if (phase == Phase.NONE
        && minecraft.level.getBlockState(placePos).getBlock() != Blocks.AIR) {
      rotateTo(placePos);
      minecraft.gameMode.continueDestroyBlock(placePos, Direction.UP);
      return;
    }
    rotateTo(placePos);
    switch (phase) {
      case NONE -> {
        placeFluid(Items.LAVA_BUCKET);
        phase = Phase.LAVA_DOWN;
      }
      case LAVA_DOWN -> {
        placeFluid(Items.BUCKET);
        phase = Phase.LAVA_UP;
      }
      case LAVA_UP -> {
        placeFluid(Items.WATER_BUCKET);
        phase = Phase.WATER_DOWN;
      }
      case WATER_DOWN -> {
        placeFluid(Items.BUCKET);
        phase = Phase.WATER_UP;
      }
      case WATER_UP -> {
        dist--;
        placeFluid(Items.LAVA_BUCKET);
        phase = Phase.LAVA_DOWN;
      }
    }
    setTag("Lavacast [" + phase + " " + dist + "]");
  }

  private boolean waiting() {
    if (phase == Phase.LAVA_DOWN && tick < dist * lavaDownMult.getValue()) return true;
    if (phase == Phase.LAVA_UP && tick < dist * lavaUpMult.getValue()) return true;
    if (phase == Phase.WATER_DOWN && tick < dist * waterDownMult.getValue()) return true;
    if (phase == Phase.WATER_UP && tick < dist * waterUpMult.getValue()) return true;
    return tick < tickInterval.getValue();
  }

  private void rotateTo(BlockPos pos) {
    PlayerUtil.setRotation(PlayerUtil.getYaw(pos), PlayerUtil.getPitch(pos));
  }

  private void placeFluid(net.minecraft.world.item.Item item) {
    int slot = PlayerUtil.findInHotbar(s -> !s.isEmpty() && s.getItem() == item);
    if (slot == -1) {
      setRunning(false);
      return;
    }
    boolean swapped = PlayerUtil.swapToSlot(slot, true);
    minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
    PlayerUtil.swingHand();
    PlayerUtil.swapBackIf(swapped);
  }

  private BlockPos targetBlock() {
    if (!(minecraft.hitResult instanceof BlockHitResult hit)) return null;
    if (hit.getType() != HitResult.Type.BLOCK) return null;
    return hit.getBlockPos();
  }

  private void measure(Vec3i offset) {
    BlockPos pos = placePos.below().offset(offset);
    BlockHitResult result =
        minecraft.level.clip(
            new ClipContext(
                Vec3.atCenterOf(pos),
                Vec3.atCenterOf(pos.below(250)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                minecraft.player));
    if (result == null || result.getType() != HitResult.Type.BLOCK) return;
    int d = placePos.getY() - result.getBlockPos().getY();
    if (d > dist) dist = d;
  }

  private void onRender(RenderWorldEvent event) {
    if (placePos == null) return;
    int color =
        switch (phase) {
          case LAVA_DOWN -> 0xFFFFB40A;
          case LAVA_UP -> 0xFFFFB480;
          case WATER_DOWN -> 0xFF0A0AFF;
          case WATER_UP -> 0xFF8080FF;
          default -> 0xFF808080;
        };
    Render3D.drawBoxOutline(
        event.getSubmitNodeStorage(),
        event.getCamera(),
        event.getMatrixStack(),
        new AABB(placePos),
        color,
        2.0f);
  }
}
