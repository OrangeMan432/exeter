package me.friendly.exeter.module.impl.toggle.world;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.WorldProperties;

public class NoWeather extends ToggleableModule {

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("noweather_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          NoWeather.this.onTick();
        }
      };

  public NoWeather() {
    super("NoWeather", new String[] {"noweather", "no-weather", "clearweather"}, 0xAAAAAA, ModuleType.WORLD);
    setDescription("Clears rain, snow and thunder.");
    offerProperties();
    this.listeners.add(tickListener);
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.world == null) {
      return;
    }
    // Let vanilla schedule weather again soon.
    WorldProperties props = mc.world.method_262();
    props.setRainTime(1200);
    props.getThunderTime(1200);
  }

  private void onTick() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.world == null) {
      return;
    }
    WorldProperties props = mc.world.method_262();
    if (props.getRaining()) {
      props.setRaining(false);
    }
    if (props.getThundering()) {
      props.setThundering(false);
    }
    // Re-applied every tick so server weather packets never stick.
    props.setRainTime(1000000);
    props.getThunderTime(1000000);
  }
}
