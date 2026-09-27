package me.friendly.exeter.module.impl.toggle.world.fakeplayer;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.FakePlayerEntity;
import me.friendly.exeter.module.impl.toggle.world.fakeplayer.util.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class ListenerTick extends Listener<TickEvent> {
  private static final Minecraft mc = Minecraft.getInstance();
  private final FakePlayerModule module;
  private boolean wasRecording;
  private int ticks;

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

  private void tickRegen(FakePlayerEntity fp) {
    MobEffectInstance regen = fp.getEffect(MobEffects.REGENERATION);
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
