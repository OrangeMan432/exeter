package me.larp.client.module.impl.toggle.misc;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

/** Instant respawn on the death screen with an optional delay. */
public class AutoRespawn extends ToggleableModule {

  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(10, 0, 100, "Delay");

  private int deathTicks = -1;

  public AutoRespawn() {
    super("AutoRespawn", new String[] {"autorespawn", "respawn"}, 0x00FFFF,
        ModuleType.MISCELLANEOUS);
    setDescription("Respawns immediately on death.");
    offerProperties(delay);
    this.listeners.add(
        new Listener<TickEvent>("autorespawn_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoRespawn.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    deathTicks = -1;
  }

  private void onTick() {
    if (minecraft.player == null || minecraft.getConnection() == null) return;
    if (minecraft.gui.screen() instanceof DeathScreen) {
      deathTicks++;
      if (deathTicks >= delay.getValue()) {
        minecraft.getConnection().send(
            new ServerboundClientCommandPacket(
                ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        deathTicks = -1000;
      }
    } else {
      deathTicks = -1;
    }
  }
}
