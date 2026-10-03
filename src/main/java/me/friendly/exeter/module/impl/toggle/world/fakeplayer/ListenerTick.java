package me.friendly.exeter.module.impl.toggle.world.fakeplayer;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.FakePlayerEntity;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ListenerTick extends Listener<TickEvent> {
  private static final Minecraft mc = Minecraft.getInstance();
  private final FakePlayerModule module;
  private boolean wasRecording;
  private int ticks;
  private final java.util.Map<Integer, TrackedRocket> rockets = new java.util.HashMap<>();

  /** Last-seen state of a rocket, so its detonation can be scored once it vanishes. */
  private record TrackedRocket(Vec3 pos, int bursts) {}

  public ListenerTick(FakePlayerModule module) {
    super("fakeplayer_tick");
    this.module = module;
  }

  @Override
  public void call(TickEvent event) {
    if (event.getStage() != Stage.PRE) return;
    if (mc.level == null || mc.player == null) return;

    module.checkRespawn();

    if (module.getFakePlayer() == null) return;

    if (module.getFakePlayer().isDeadOrDying()) {
      module.onFakePlayerDied();
      return;
    }

    FakePlayerEntity fp = module.getFakePlayer();

    tickRegen(fp);
    tickFireworks(fp);

    boolean record = module.isRecording();

    if (module.isGappleEnabled() && module.getTimer().hasPassed(module.getGappleDelay())) {
      fp.setAbsorptionAmount(16.0f);
      fp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
      fp.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0));
      fp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0));
      fp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));
      module.getTimer().reset();
    }

    if (!record) {
      if (module.isPlayingRecording()) {
        if (module.getPositions().isEmpty()) {
          module.setPlayingRecording(false);
          return;
        }

        if (module.getIndex() >= module.getPositions().size()) {
          if (!module.isLooping()) {
            module.setPlayingRecording(false);
          }
          module.setIndex(0);
        }

        if (ticks++ % 2 == 0) {
          Position p = module.getPositions().get(module.getIndex());
          module.setIndex(module.getIndex() + 1);
          fp.snapTo(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
          fp.setMotionDirect(p.getMotionX(), p.getMotionY(), p.getMotionZ());
        }
      } else {
        module.setIndex(0);
        fp.setMotionDirect(0, 0, 0);
      }
    } else if (record) {
      module.setPlayingRecording(false);
      fp.setMotionDirect(0, 0, 0);

      if (!wasRecording) {
        module.getPositions().clear();
        wasRecording = true;
      }

      if (ticks++ % 2 == 0) {
        module.getPositions().add(new Position(mc.player));
      }
    }
  }

  /**
   * Firework rockets hurt through direct hurt() calls with no explosion packet, so the explosion
   * listener never sees them. A rocket that vanishes near the dummy detonated: score it with the
   * vanilla formula (5 + 2 per burst, 5m radius, sqrt falloff, feet/mid-body line of sight).
   */
  private void tickFireworks(FakePlayerEntity fp) {
    if (!module.isDamageEnabled()) {
      rockets.clear();
      return;
    }
    java.util.Set<Integer> seen = new java.util.HashSet<>();
    for (var entity : mc.level.entitiesForRendering()) {
      if (!(entity instanceof FireworkRocketEntity rocket)) continue;
      seen.add(entity.getId());
      rockets.put(entity.getId(), new TrackedRocket(entity.position(), burstCount(rocket)));
    }
    var it = rockets.entrySet().iterator();
    while (it.hasNext()) {
      var entry = it.next();
      if (seen.contains(entry.getKey())) continue;
      it.remove();
      applyFireworkDamage(fp, entry.getValue());
    }
    if (rockets.size() > 64) {
      rockets.clear();
    }
  }

  private static int burstCount(FireworkRocketEntity rocket) {
    var fireworks = rocket.getItem().get(DataComponents.FIREWORKS);
    return fireworks == null ? 0 : fireworks.explosions().size();
  }

  private void applyFireworkDamage(FakePlayerEntity fp, TrackedRocket rocket) {
    double distance = rocket.pos().distanceTo(fp.position());
    me.friendly.exeter.logging.DebugLogger.get()
        .logFile(
            "FakePlayer",
            "rocket vanished " + Math.round(distance) + "m from dummy hp=" + fp.getHealth());
    if (distance > 5.0) return;
    float base = 5.0F + 2.0F * rocket.bursts();
    if (base <= 0.0F) return;
    if (!hasLineOfSight(fp, rocket.pos())) {
      me.friendly.exeter.logging.DebugLogger.get()
          .logFile("FakePlayer", "no line of sight, no damage (vanilla deals 0)");
      return;
    }
    float damage = (float) (base * Math.sqrt((5.0 - distance) / 5.0));
    if (damage > 0) {
      me.friendly.exeter.logging.DebugLogger.get()
          .logFile("FakePlayer", "applying " + damage + " firework damage (vanilla formula)");
      fp.applyDamage(damage);
    }
  }

  /** Vanilla checks feet and mid-body rays with COLLIDER blocks; either clear deals damage. */
  private boolean hasLineOfSight(FakePlayerEntity fp, Vec3 from) {
    for (double scale : new double[] {0.0, 0.5}) {
      Vec3 to = new Vec3(fp.getX(), fp.getY(scale), fp.getZ());
      var hit =
          mc.level.clip(
              new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, fp));
      if (hit.getType() == HitResult.Type.MISS) return true;
    }
    return false;
  }

  private void tickRegen(FakePlayerEntity fp) {
    MobEffectInstance regen = fp.getEffect(MobEffects.REGENERATION);
    if (regen == null) {
      return;
    }
    // Vanilla intervals: 50 ticks per heart at I, halved per amplifier.
    int interval = Math.max(1, 50 >> Math.min(regen.getAmplifier(), 5));
    if (fp.tickCount % interval != 0) {
      return;
    }
    float newHealth = Math.min(fp.getHealth() + 1.0F, fp.getMaxHealth());
    fp.setHealth(newHealth);
  }
}
