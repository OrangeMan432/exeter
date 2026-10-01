package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.EnumProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class FullBright extends ToggleableModule {

  private enum Mode {
    GAMMA,
    NIGHT_VISION
  }

  private static final double BRIGHT_GAMMA = 16.0;

  private final EnumProperty<Mode> mode = new EnumProperty<>(Mode.GAMMA, "Mode", "mode");

  private double savedGamma = 1.0;
  private boolean savedGammaValid;

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("fullbright_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          onTick();
        }
      };

  public FullBright() {
    super("FullBright", new String[] {"fullbright", "fb", "bright"}, 0xFFFFAA, ModuleType.RENDER);
    setDescription("Maximizes brightness.");
    offerProperties(mode);
    listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    savedGammaValid = false;
    applyGamma();
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    restoreGamma();
    removeNightVision();
  }

  private void onTick() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null) return;
    if (mode.getValue() == Mode.GAMMA) {
      removeNightVision();
      if (mc.options.gamma().get() != BRIGHT_GAMMA) {
        applyGamma();
      }
    } else {
      restoreGamma();
      if (!mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
        mc.player.addEffect(
            new MobEffectInstance(MobEffects.NIGHT_VISION, 1000000, 0, false, false));
      }
    }
  }

  private void applyGamma() {
    Minecraft mc = Minecraft.getInstance();
    if (!savedGammaValid) {
      savedGamma = mc.options.gamma().get();
      savedGammaValid = true;
    }
    mc.options.gamma().set(BRIGHT_GAMMA);
  }

  private void restoreGamma() {
    if (!savedGammaValid) return;
    savedGammaValid = false;
    Minecraft.getInstance().options.gamma().set(savedGamma);
  }

  private void removeNightVision() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player != null && mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
      mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }
  }
}
