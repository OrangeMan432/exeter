package me.larp.client.module.impl.toggle.movement;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Orbits the nearest target at range while you hold forward. */
public class TargetStrafe extends ToggleableModule {

  private final NumberProperty<Double> range = new NumberProperty<Double>(2.5, 0.5, 6.0, "Range");
  private final NumberProperty<Double> speed = new NumberProperty<Double>(0.35, 0.05, 1.0, "Speed");
  private final Property<Boolean> direction = new Property<Boolean>(true, "Direction");
  private final Property<Boolean> onlyForward = new Property<Boolean>(true, "Only Forward");
  private final Property<Boolean> autoJump = new Property<Boolean>(false, "Auto Jump");

  public TargetStrafe() {
    super("TargetStrafe", new String[] {"targetstrafe", "strafe"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Strafes around your target in fights.");
    offerProperties(range, speed, direction, onlyForward, autoJump);
    this.listeners.add(
        new Listener<TickEvent>("targetstrafe_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            TargetStrafe.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    if (onlyForward.getValue() && !PlayerUtil.isMoving()) return;

    Player target = findTarget();
    if (target == null) return;

    double dx = minecraft.player.getX() - target.getX();
    double dz = minecraft.player.getZ() - target.getZ();
    double dist = Math.sqrt(dx * dx + dz * dz);
    if (dist < 0.01) return;

    // Tangent direction for the orbit, radial correction to hold range.
    double dir = direction.getValue() ? 1.0 : -1.0;
    double tx = (-dz / dist) * dir;
    double tz = (dx / dist) * dir;
    double radial = (dist - range.getValue()) * 0.4;
    double rx = (dx / dist) * -radial;
    double rz = (dz / dist) * -radial;

    double mx = (tx + rx) * speed.getValue() * 3.0;
    double mz = (tz + rz) * speed.getValue() * 3.0;
    minecraft.player.setDeltaMovement(mx, minecraft.player.getDeltaMovement().y, mz);

    if (autoJump.getValue() && minecraft.player.onGround() && !minecraft.player.isInWater()) {
      minecraft.player.jumpFromGround();
    }
  }

  private Player findTarget() {
    Player best = null;
    double bestDist = range.getValue() + 3.0;
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
}
