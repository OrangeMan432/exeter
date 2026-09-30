package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.block.Material;
import net.minecraft.entity.player.PlayerEntity;

public class FastFall extends ToggleableModule {

  private final NumberProperty<Double> factor =
      new NumberProperty<Double>(1.5, 1.0, 3.0, "Factor", "factor");

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("fastfall_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          FastFall.this.onTick();
        }
      };

  public FastFall() {
    super("FastFall", new String[] {"fastfall", "fast fall"}, 0x00AAFF, ModuleType.MOVEMENT);
    setDescription("Makes you fall faster.");
    offerProperties(factor);
    this.listeners.add(tickListener);
  }

  private void onTick() {
    if (minecraft() == null || minecraft().player == null) {
      return;
    }
    PlayerEntity player = minecraft().player;
    if (player.field_1623) {
      // onGround
      return;
    }
    if (player.velocityY >= -0.1) {
      return;
    }
    if (player.isInFluid(Material.WATER) || player.isInFluid(Material.LAVA)) {
      return;
    }
    player.velocityY *= factor.getValue().doubleValue();
  }
}
