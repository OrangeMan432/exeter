package me.larp.client.module.impl.toggle.combat;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Meteor-Rejects-pattern AimAssist: smoothly rotates toward the best target
 * in range and FOV, head or body, instant or speed-capped.
 */
public class AimAssist extends ToggleableModule {

  public enum AimPoint {
    HEAD,
    BODY
  }

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(5.0, 1.0, 10.0, "Range");
  private final NumberProperty<Double> fov =
      new NumberProperty<Double>(360.0, 10.0, 360.0, "FOV");
  private final Property<Boolean> ignoreWalls =
      new Property<Boolean>(false, "Ignore Walls");
  private final Property<Boolean> targetPlayers =
      new Property<Boolean>(true, "Players");
  private final Property<Boolean> targetHostiles =
      new Property<Boolean>(true, "Hostiles");
  private final EnumProperty<AimPoint> aimPoint =
      new EnumProperty<AimPoint>(AimPoint.BODY, "Aim Point");
  private final Property<Boolean> instant =
      new Property<Boolean>(false, "Instant");
  private final NumberProperty<Double> speed =
      new NumberProperty<Double>(5.0, 0.5, 30.0, "Speed");

  public AimAssist() {
    super("AimAssist", new String[] {"aimassist", "aim-assist"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Smoothly aims at targets for you.");
    offerProperties(
        range, fov, ignoreWalls, targetPlayers, targetHostiles, aimPoint, instant, speed);
    this.listeners.add(
        new Listener<TickEvent>("aimassist_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AimAssist.this.onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    LivingEntity target = findTarget();
    if (target == null) {
      setTag("AimAssist");
      return;
    }
    setTag("AimAssist [" + target.getName().getString() + "]");
    aim(target);
  }

  private LivingEntity findTarget() {
    double rangeSq = range.getValue() * range.getValue();
    LivingEntity best = null;
    double bestScore = Double.MAX_VALUE;
    for (Entity e : minecraft.level.entitiesForRendering()) {
      if (!(e instanceof LivingEntity) || e == minecraft.player || !e.isAlive()) continue;
      if (e instanceof Player p) {
        if (!targetPlayers.getValue()) continue;
        if (me.larp.client.core.Larp.getInstance()
            .getFriendManager()
            .isFriend(p.getName().getString())) continue;
      } else if (e instanceof Enemy) {
        if (!targetHostiles.getValue()) continue;
      } else {
        continue;
      }
      double distSq = minecraft.player.distanceToSqr(e);
      if (distSq > rangeSq) continue;
      if (!ignoreWalls.getValue() && !minecraft.player.hasLineOfSight(e)) continue;
      if (!inFov(e)) continue;
      double score =
          ((LivingEntity) e).getHealth() + ((LivingEntity) e).getAbsorptionAmount();
      if (score < bestScore) {
        bestScore = score;
        best = (LivingEntity) e;
      }
    }
    return best;
  }

  private boolean inFov(Entity target) {
    double fovValue = fov.getValue();
    if (fovValue >= 360.0) return true;
    double dx = target.getX() - minecraft.player.getX();
    double dz = target.getZ() - minecraft.player.getZ();
    double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
    double diff =
        Math.abs(
            net.minecraft.util.Mth.wrapDegrees(
                (float) (targetYaw - minecraft.player.getYRot())));
    return diff <= fovValue / 2.0;
  }

  private void aim(LivingEntity target) {
    double aimHeight =
        aimPoint.getValue() == AimPoint.HEAD
            ? target.getEyeHeight()
            : target.getEyeHeight() / 2.0;
    double dx = target.getX() - minecraft.player.getX();
    double dy =
        (target.getY() + aimHeight)
            - (minecraft.player.getY() + minecraft.player.getEyeHeight());
    double dz = target.getZ() - minecraft.player.getZ();

    float wantYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    double dist = Math.sqrt(dx * dx + dz * dz);
    float wantPitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));

    if (instant.getValue()) {
      minecraft.player.setYRot(wantYaw);
      minecraft.player.setXRot(wantPitch);
      return;
    }
    double step = speed.getValue();
    float yawDiff = net.minecraft.util.Mth.wrapDegrees(wantYaw - minecraft.player.getYRot());
    float yawMove = (float) Math.copySign(Math.min(Math.abs(yawDiff), step), yawDiff);
    float pitchDiff = net.minecraft.util.Mth.wrapDegrees(wantPitch - minecraft.player.getXRot());
    float pitchMove = (float) Math.copySign(Math.min(Math.abs(pitchDiff), step), pitchDiff);
    minecraft.player.setYRot(minecraft.player.getYRot() + yawMove);
    minecraft.player.setXRot(minecraft.player.getXRot() + pitchMove);
  }
}
