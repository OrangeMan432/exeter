package me.larp.client.module.impl.toggle.render;

import java.util.ArrayList;
import java.util.List;
import me.larp.api.event.Listener;
import me.larp.client.events.RenderWorldEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.Render3D;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Simulates thrown projectiles and draws the landing path. */
public class Trajectories extends ToggleableModule {

  private final NumberProperty<Double> lineWidth =
      new NumberProperty<Double>(2.0, 1.0, 5.0, "Line Width");
  private final NumberProperty<Integer> steps =
      new NumberProperty<Integer>(100, 20, 200, "Steps");
  private final Property<Boolean> pearls = new Property<Boolean>(true, "Pearls");
  private final Property<Boolean> bows = new Property<Boolean>(true, "Bows");
  private final Property<Boolean> landingBox =
      new Property<Boolean>(true, "Landing Box");

  public Trajectories() {
    super("Trajectories", new String[] {"trajectories", "traj"}, 0xFF00FF, ModuleType.RENDER);
    setDescription("Shows where projectiles land.");
    offerProperties(lineWidth, steps, pearls, bows, landingBox);
    this.listeners.add(
        new Listener<RenderWorldEvent>("trajectories_render") {
          @Override
          public void call(RenderWorldEvent event) {
            Trajectories.this.onRender(event);
          }
        });
  }

  private void onRender(RenderWorldEvent event) {
    if (minecraft.level == null || minecraft.player == null) return;
    ItemStack held = minecraft.player.getMainHandItem();
    if (held.isEmpty()) return;

    double power;
    double gravity;
    double drag;
    if (held.getItem() == Items.ENDER_PEARL && pearls.getValue()) {
      power = 1.5;
      gravity = 0.03;
      drag = 0.99;
    } else if (held.getItem() instanceof BowItem && bows.getValue()
        && minecraft.player.isUsingItem()) {
      int useTicks = minecraft.player.getTicksUsingItem();
      float charge = Math.min(useTicks / 20.0f, 1.0f);
      power = charge * 3.0;
      gravity = 0.05;
      drag = 0.99;
      if (power < 0.2) return;
    } else {
      return;
    }

    Vec3 look = minecraft.player.getViewVector(1.0f);
    Vec3 pos = minecraft.player.getEyePosition().subtract(0, 0.1, 0);
    Vec3 motion = look.scale(power);
    List<Vec3> path = new ArrayList<>();
    path.add(pos);
    Vec3 landing = null;
    for (int i = 0; i < steps.getValue(); i++) {
      Vec3 next = pos.add(motion);
      ClipContext ctx =
          new ClipContext(
              pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player);
      if (minecraft.level.clip(ctx).getType() != HitResult.Type.MISS) {
        landing = next;
        path.add(next);
        break;
      }
      path.add(next);
      pos = next;
      motion = motion.scale(drag).subtract(0, gravity, 0);
    }
    Render3D.drawPath(
        event.getSubmitNodeStorage(),
        event.getCamera(),
        event.getMatrixStack(),
        path,
        0xFF00FFFF,
        lineWidth.getValue().floatValue());
    if (landingBox.getValue() && landing != null) {
      net.minecraft.core.BlockPos block =
          new net.minecraft.core.BlockPos(
              (int) Math.floor(landing.x), (int) Math.floor(landing.y), (int) Math.floor(landing.z));
      Render3D.drawBoxOutline(
          event.getSubmitNodeStorage(),
          event.getCamera(),
          event.getMatrixStack(),
          new net.minecraft.world.phys.AABB(block),
          0xFF00FFFF,
          lineWidth.getValue().floatValue());
    }
  }
}
