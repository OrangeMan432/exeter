package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.block.Material;
import net.minecraft.entity.player.PlayerEntity;

public class NoAccel extends ToggleableModule {

  private final NumberProperty<Double> speed =
      new NumberProperty<Double>(0.22, 0.1, 0.5, "Speed", "speed");

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("noaccel_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          NoAccel.this.onTick();
        }
      };

  public NoAccel() {
    super("NoAccel", new String[] {"noaccel"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Removes movement acceleration.");
    offerProperties(speed);
    this.listeners.add(tickListener);
  }

  private void onTick() {
    if (minecraft() == null || minecraft().player == null) {
      return;
    }
    PlayerEntity player = minecraft().player;
    if (!player.field_1623) {
      // onGround only
      return;
    }
    if (!PlayerUtil.isMoving()) {
      return;
    }
    if (player.isInFluid(Material.WATER) || player.isInFluid(Material.LAVA)) {
      return;
    }
    double current = Math.sqrt(player.velocityX * player.velocityX + player.velocityZ * player.velocityZ);
    if (current < 0.01) {
      return;
    }
    double target = speed.getValue().doubleValue();
    player.velocityX = player.velocityX / current * target;
    player.velocityZ = player.velocityZ / current * target;
  }
}
