package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public final class NoFall extends ToggleableModule {

  private final NumberProperty<Double> minDistance =
      new NumberProperty<Double>(3.0, 0.0, 20.0, "Min Distance");

  public NoFall() {
    super("NoFall", new String[] {"nofall", "nofalldamage"}, 0x00FFAA, ModuleType.MOVEMENT);
    setDescription("Prevents fall damage when above a minimum height.");
    offerProperties(minDistance);
    listeners.add(
        new Listener<TickEvent>("nofall_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            onTick();
          }
        });
  }

  private void onTick() {
    if (minecraft.player == null || minecraft.level == null) return;
    if (minecraft.player.isCreative() || minecraft.player.isSpectator()) return;
    if (minecraft.player.isFallFlying()) return;
    double fall = minecraft.player.fallDistance;
    if (fall > minDistance.getValue()
        && !minecraft.player.onGround()
        && minecraft.player.getDeltaMovement().y < -0.1) {
      minecraft.player.fallDistance = 0;
      // spoof onGround to prevent server fall damage calculation
      try {
        if (minecraft.getConnection() != null) {
          minecraft.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, false));
        }
      } catch (Exception ignored) {
      }
    } else if (fall > minDistance.getValue()) {
      // still reset even if not falling fast to avoid accumulated fall
      minecraft.player.fallDistance = 0;
    }
  }
}
