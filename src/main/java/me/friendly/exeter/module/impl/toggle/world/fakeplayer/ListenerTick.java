package me.friendly.exeter.module.impl.toggle.world.fakeplayer;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.FakePlayerEntity;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.Position;
import me.friendly.exeter.util.ExplosionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
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
    float partial = me.friendly.exeter.render.ModelWireframe.currentPartialTick();

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

        int size = module.getPositions().size();
        if (module.getIndex() >= size || module.getIndex() < 0) {
          if (!module.isLooping()) {
            module.setPlayingRecording(false);
            module.setIndex(0);
            return;
          } else if (module.isReverse()) {
            // Reverse is a second pass over the same frames, not a restart: that is what makes
            // a recording of walking up stairs run back down them.
            module.setReverseRun(!module.isReverseRun());
            module.setIndex(module.isReverseRun() ? size - 1 : 0);
          } else {
            module.setIndex(0);
          }
        }

        if (ticks++ % 2 == 0) {
          int i = module.getIndex();
          // Recomputed after the flip above, so the turn frame steps the right way.
          boolean reversed = module.isReverse() && module.isReverseRun();
          if (reversed) {
            module.setIndex(i - 1);
          } else {
            module.setIndex(i + 1);
          }
          Position p = module.getPositions().get(Math.max(0, Math.min(size - 1, i)));
          fp.snapTo(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
          fp.setMotionDirect(p.getMotionX(), p.getMotionY(), p.getMotionZ());
          applyLook(fp, p);
          applyLimbs(fp, p);
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
        module.getPositions().add(new Position(mc.player, partial));
      }
    }
  }

  /** Head and body angle, so the dummy looks where the recording looked. */
  private static void applyLook(FakePlayerEntity fp, Position p) {
    fp.setYHeadRot(p.getHeadYaw());
    fp.setYBodyRot(p.getHeadYaw());
  }

  /**
   * Limb positions: walk cycle speed drives the leg swing, and a recorded swing restarts the arm
   * animation when the recording crosses into one.
   */
  private static void applyLimbs(FakePlayerEntity fp, Position p) {
    // speed straight from the recording, position advances from there: the only public path into
    // WalkAnimationState, and it keeps the phase in step with the movement.
    fp.walkAnimation.update(p.getWalkSpeed(), 1.0f, 1.0f);
    if (p.isSwinging()) {
      var hand =
          p.isOffhandSwing()
              ? net.minecraft.world.InteractionHand.OFF_HAND
              : net.minecraft.world.InteractionHand.MAIN_HAND;
      if (!fp.isSwinging() || swingHand(fp) != hand) {
        fp.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
      }
    }
  }

  private static net.minecraft.world.InteractionHand swingHand(FakePlayerEntity fp) {
    var swing = fp.getCurrentSwing();
    return swing == null ? net.minecraft.world.InteractionHand.MAIN_HAND : swing.hand();
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
    return ExplosionUtil.fireworkBursts(rocket.getItem());
  }

  private void applyFireworkDamage(FakePlayerEntity fp, TrackedRocket rocket) {
    double distance = rocket.pos().distanceTo(fp.position());
    me.friendly.exeter.logging.DebugLogger.get()
        .logFile(
            "FakePlayer",
            "rocket vanished " + Math.round(distance) + "m from dummy hp=" + fp.getHealth());
    if (distance > 5.0 || rocket.bursts() <= 0) return;
    if (!ExplosionUtil.hasLineOfSight(mc.level, rocket.pos(), fp)) {
      me.friendly.exeter.logging.DebugLogger.get()
          .logFile("FakePlayer", "no line of sight, no damage (vanilla deals 0)");
      return;
    }
    float damage = ExplosionUtil.fireworkDamage(mc.level, rocket.pos(), rocket.bursts(), fp);
    if (damage > 0) {
      me.friendly.exeter.logging.DebugLogger.get()
          .logFile("FakePlayer", "applying " + damage + " firework damage (vanilla formula)");
      fp.applyDamage(damage);
    }
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
