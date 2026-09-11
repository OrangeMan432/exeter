package me.larp.client.module.impl.toggle.movement;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.core.Larp;
import me.larp.client.events.RenderWorldEvent;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.Render3D;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Meteor-Rejects-pattern Confuse: teleports around enemies to break their aim. */
public class Confuse extends ToggleableModule {

  public enum Mode {
    RANDOM_TP,
    SWITCH,
    CIRCLE
  }

  private final EnumProperty<Mode> mode = new EnumProperty<Mode>(Mode.RANDOM_TP, "Mode");
  private final NumberProperty<Integer> delay = new NumberProperty<Integer>(3, 0, 20, "Delay");
  private final NumberProperty<Double> range = new NumberProperty<Double>(6.0, 1.0, 10.0, "Range");
  private final NumberProperty<Integer> circleSpeed =
      new NumberProperty<Integer>(10, 1, 180, "Circle Speed");
  private final Property<Boolean> moveThroughBlocks =
      new Property<Boolean>(false, "Move Through Blocks");

  private final Random random = new Random();
  private int delayWaited;
  private double circleProgress;
  private Player target;

  public Confuse() {
    super("Confuse", new String[] {"confuse"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Teleports around enemies to confuse them.");
    offerProperties(mode, delay, range, circleSpeed, moveThroughBlocks);
    this.listeners.add(
        new Listener<TickEvent>("confuse_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Confuse.this.onTick();
          }
        });
    this.listeners.add(
        new Listener<RenderWorldEvent>("confuse_render") {
          @Override
          public void call(RenderWorldEvent event) {
            Confuse.this.onRender(event);
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    delayWaited = 0;
    circleProgress = 0;
    target = null;
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    target = null;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    boolean circle = mode.getValue() == Mode.CIRCLE;
    if (!circle) {
      delayWaited++;
      if (delayWaited < delay.getValue()) return;
      delayWaited = 0;
    }

    target = findTarget();
    if (target == null) return;

    Vec3 entityPos = target.position();
    Vec3 playerPos = minecraft.player.position();
    double halfRange = range.getValue() / 2.0;

    switch (mode.getValue()) {
      case RANDOM_TP -> {
        double x = random.nextDouble() * range.getValue() - halfRange;
        double z = random.nextDouble() * range.getValue() - halfRange;
        Vec3 goal = entityPos.add(new Vec3(x, 0, z));
        if (minecraft.level.getBlockState(BlockPos.containing(goal.x, goal.y, goal.z)).getBlock()
            != Blocks.AIR) {
          goal = new Vec3(entityPos.x + x, playerPos.y, entityPos.z + z);
        }
        if (minecraft.level.getBlockState(BlockPos.containing(goal.x, goal.y, goal.z)).getBlock()
            == Blocks.AIR) {
          BlockHitResult hit =
              minecraft.level.clip(
                  new ClipContext(
                      playerPos, goal, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                      minecraft.player));
          if (!moveThroughBlocks.getValue() && hit.isInside()) {
            delayWaited = delay.getValue() - 1;
            break;
          }
          minecraft.player.absSnapTo(goal.x, goal.y, goal.z);
        } else {
          delayWaited = delay.getValue() - 1;
        }
      }
      case SWITCH -> {
        Vec3 diff = entityPos.subtract(playerPos);
        Vec3 clamped =
            new Vec3(
                Mth.clamp(diff.x, -halfRange, halfRange),
                Mth.clamp(diff.y, -halfRange, halfRange),
                Mth.clamp(diff.z, -halfRange, halfRange));
        Vec3 goal = entityPos.add(clamped);
        BlockHitResult hit =
            minecraft.level.clip(
                new ClipContext(
                    playerPos, goal, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                    minecraft.player));
        if (!moveThroughBlocks.getValue() && hit.isInside()) {
          delayWaited = delay.getValue() - 1;
          break;
        }
        minecraft.player.absSnapTo(goal.x, goal.y, goal.z);
      }
      case CIRCLE -> {
        circleProgress += circleSpeed.getValue();
        if (circleProgress > 360) circleProgress -= 360;
        double rad = Math.toRadians(circleProgress);
        Vec3 goal =
            new Vec3(
                entityPos.x + Math.sin(rad) * 3.0, playerPos.y, entityPos.z + Math.cos(rad) * 3.0);
        BlockHitResult hit =
            minecraft.level.clip(
                new ClipContext(
                    playerPos, goal, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                    minecraft.player));
        if (!moveThroughBlocks.getValue() && hit.isInside()) break;
        minecraft.player.absSnapTo(goal.x, goal.y, goal.z);
      }
    }
  }

  private Player findTarget() {
    Player best = null;
    double bestHealth = Double.MAX_VALUE;
    for (Entity entity : minecraft.level.players()) {
      if (!(entity instanceof Player player)) continue;
      if (player == minecraft.player || !player.isAlive()) continue;
      if (minecraft.player.distanceTo(player) > range.getValue()) continue;
      if (Larp.getInstance().getFriendManager().isFriend(player.getName().getString())) continue;
      float health = player.getHealth() + player.getAbsorptionAmount();
      if (health < bestHealth) {
        bestHealth = health;
        best = player;
      }
    }
    return best;
  }

  private void onRender(RenderWorldEvent event) {
    if (minecraft.level == null || minecraft.player == null) return;
    if (target == null || !target.isAlive()) return;
    Vec3 base = target.position().add(0, target.getBbHeight() / 2.0, 0);
    List<Vec3> points = new ArrayList<>(65);
    for (int i = 0; i <= 64; i++) {
      double rad = Math.toRadians((i / 64.0) * 360.0);
      points.add(
          new Vec3(base.x + Math.sin(rad) * 3.0, base.y, base.z + Math.cos(rad) * 3.0));
    }
    Render3D.drawPath(
        event.getSubmitNodeStorage(), event.getCamera(), event.getMatrixStack(), points,
        0xFF00FF00, 2.0f);
  }
}
