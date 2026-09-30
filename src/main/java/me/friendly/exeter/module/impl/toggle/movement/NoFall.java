package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.beta.mixin.EntityAccessor;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.class_489;
import net.minecraft.client.network.MultiplayerClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

public class NoFall extends ToggleableModule {

  private final NumberProperty<Double> minDistance =
      new NumberProperty<Double>(3.0, 0.0, 20.0, "Min Distance", "mindistance");

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("nofall_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          NoFall.this.onTick();
        }
      };

  public NoFall() {
    super("NoFall", new String[] {"nofall", "nofalldamage"}, 0x00FFAA, ModuleType.MOVEMENT);
    setDescription("Prevents fall damage when above a minimum height.");
    offerProperties(minDistance);
    this.listeners.add(tickListener);
  }

  private void onTick() {
    if (minecraft() == null || minecraft().player == null) {
      return;
    }
    PlayerEntity player = minecraft().player;
    float fall = ((EntityAccessor) player).getFallDistance();
    if (fall <= minDistance.getValue().doubleValue()) {
      return;
    }
    ((EntityAccessor) player).setFallDistance(0.0F);
    if (!player.field_1623
        && player.velocityY < -0.1
        && player instanceof MultiplayerClientPlayerEntity) {
      // onGround spoof: PlayerLook packet (class_489) with onGround=true resets server fall.
      MultiplayerClientPlayerEntity mp = (MultiplayerClientPlayerEntity) player;
      if (mp.networkHandler != null) {
        mp.networkHandler.sendPacket(new class_489(player.yaw, player.pitch, true));
      }
    }
  }
}
