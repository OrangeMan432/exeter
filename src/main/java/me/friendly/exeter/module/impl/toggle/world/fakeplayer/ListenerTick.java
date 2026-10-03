package me.friendly.exeter.module.impl.toggle.world.fakeplayer;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.FakePlayerEntity;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.Position;
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
  private final java.util.Map<Integer, Vec3> rockets = new java.util.HashMap<>();

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
   * Firework rockets hurt through direct hurt() calls with no explosion packet, so the
   * explosion listener never sees them. A rocket that vanishes near the dummy detonated:
   * apply falloff damage for it.
   */
  private void tickFireworks(FakePlayerEntity fp) {
    if (!module.isDamageEnabled()) {
      rockets.clear();
      return;
    }
    java.util.Set<Integer> seen = new java.util.HashSet<>();
    for (var entity : mc.level.entitiesForRendering()) {
      if (!(entity instanceof FireworkRocketEntity)) continue;
      seen.add(entity.getId());
      rockets.put(entity.getId(), entity.position());
    }
    var it = rockets.entrySet().iterator();
    while (it.hasNext()) {
      var entry = it.next();
      if (seen.contains(entry.getKey())) continue;
      it.remove();
      double distance = entry.getValue().distanceTo(fp.position());
      if (distance > 8.0) continue;
      float damage = (float) ((1.0 - distance / 8.0) * 10.0);
      if (damage > 0) {
        fp.applyDamage(damage);
      }
    }
    if (rockets.size() > 64) {
      rockets.clear();
    }
  }

  private void tickRegen(FakePlayerEntity fp) {    MobEffectInstance regen = fp.getEffect(MobEffects.REGENERATION);
    if (regen != null) {
      float healAmount =
          switch (regen.getAmplifier()) {
            case 0 -> 0.5F;
            case 1 -> 1.0F;
            default -> Math.min((float) (2 << regen.getAmplifier()), 20.0F);
          };
      float maxHealth = fp.getMaxHealth();
      float newHealth = Math.min(fp.getHealth() + healAmount, maxHealth);
      fp.setHealth(newHealth);
    }
  }
}
