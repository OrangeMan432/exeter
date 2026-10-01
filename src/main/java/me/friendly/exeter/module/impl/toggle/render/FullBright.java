package me.friendly.exeter.module.impl.toggle.render;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import net.minecraft.class_50;
import net.minecraft.client.Minecraft;

/**
 * Beta fullbright: Beta 1.7.3 has no gamma option, so this maxes the dimension
 * brightness table (class_50.field_2178, verified as the 16-entry light table)
 * and rebuilds all chunk renderers. Reapplies on world/dimension change.
 */
public class FullBright extends ToggleableModule {

  private float[] saved;
  private class_50 lastDimension;

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("fullbright_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          FullBright.this.onTick();
        }
      };

  public FullBright() {
    super("FullBright", new String[] {"fullbright", "fb", "bright"}, 0xFFFFAA, ModuleType.RENDER);
    setDescription("Maximizes brightness.");
    offerProperties();
    this.listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    lastDimension = null;
    saved = null;
    apply();
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    restore();
    lastDimension = null;
    saved = null;
  }

  private void onTick() {
    if (!isRunning()) {
      return;
    }
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.world == null || mc.world.dimension == null) {
      return;
    }
    if (mc.world.dimension != lastDimension) {
      saved = null;
      apply();
    }
  }

  private void apply() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.world == null || mc.world.dimension == null) {
      return;
    }
    class_50 dimension = mc.world.dimension;
    if (dimension.field_2178 == null || dimension.field_2178.length == 0) {
      return;
    }
    saved = dimension.field_2178.clone();
    for (int i = 0; i < dimension.field_2178.length; i++) {
      dimension.field_2178[i] = 1.0F;
    }
    lastDimension = dimension;
    rebuild(mc);
  }

  private void restore() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.world == null || mc.world.dimension == null) {
      return;
    }
    class_50 dimension = mc.world.dimension;
    if (saved != null
        && dimension == lastDimension
        && dimension.field_2178 != null
        && dimension.field_2178.length == saved.length) {
      System.arraycopy(saved, 0, dimension.field_2178, 0, saved.length);
      rebuild(mc);
    }
  }

  private void rebuild(Minecraft mc) {
    if (mc.worldRenderer != null) {
      mc.worldRenderer.method_1537();
    }
  }
}
