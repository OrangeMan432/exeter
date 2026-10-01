package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.Module;
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

  private final EnumProperty<Mode> mode = new EnumProperty<>(Mode.GAMMA, "Mode", "mode");

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

  public static FullBright get() {
    if (Exeter.getInstance() == null) return null;
    Module module = Exeter.getInstance().getModuleManager().getModuleByAlias("fullbright");
    return module instanceof FullBright ? (FullBright) module : null;
  }

  public static boolean isGammaActive() {
    FullBright bright = get();
    return bright != null && bright.isRunning() && bright.mode.getValue() == Mode.GAMMA;
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    removeNightVision();
  }

  private void onTick() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null) return;
    if (mode.getValue() == Mode.GAMMA) {
      removeNightVision();
    } else if (!mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
      mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000000, 0, false, false));
    }
  }

  private void removeNightVision() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player != null && mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
      mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }
  }
}
