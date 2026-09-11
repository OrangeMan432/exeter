package me.larp.client.module.impl.toggle.render;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.EnumProperty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Fullbright: night vision, max gamma, or both. Restores everything on disable.
 */
public class Fullbright extends ToggleableModule {

  public enum Mode {
    NIGHT_VISION,
    GAMMA,
    BOTH
  }

  private final EnumProperty<Mode> mode =
      new EnumProperty<Mode>(Mode.BOTH, "Mode");

  private double savedGamma = 1.0;

  public Fullbright() {
    super("Fullbright", new String[] {"fullbright", "bright", "fb"}, 0xFF00FF, ModuleType.RENDER);
    setDescription("Maximum brightness everywhere.");
    offerProperties(mode);
    this.listeners.add(
        new Listener<TickEvent>("fullbright_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Fullbright.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    if (minecraft.player == null) return;
    if (mode.getValue() != Mode.NIGHT_VISION) {
      try {
        savedGamma = minecraft.options.gamma().get();
        // Option validation rejects anything above max: clamp to it.
        minecraft.options.gamma().set(1.0);
      } catch (Exception e) {
        // options unavailable: night vision below still applies
      }
    }
    applyNightVision();
  }

  private void onTick() {
    if (minecraft.player == null) return;
    if (mode.getValue() == Mode.GAMMA) return;
    // Server entity sync strips client-added effects: re-apply every tick.
    applyNightVision();
  }

  private void applyNightVision() {
    if (minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) return;
    minecraft.player.addEffect(
        new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false));
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    if (minecraft.player != null
        && minecraft.player.hasEffect(MobEffects.NIGHT_VISION)
        && mode.getValue() != Mode.GAMMA) {
      minecraft.player.removeEffect(MobEffects.NIGHT_VISION);
    }
    try {
      minecraft.options.gamma().set(savedGamma);
    } catch (Exception e) {
      // options unavailable
    }
  }
}
