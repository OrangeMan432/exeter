package me.friendly.exeter.module.impl.toggle.movement;

import me.friendly.api.event.Listener;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

public class Velocity extends ToggleableModule {

  private final Listener<PacketEvent> packetListener =
      new Listener<PacketEvent>("velocity_packet") {
        @Override
        public void call(PacketEvent event) {
          if (minecraft.player == null) return;

          if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet) {
            if (packet.id() == minecraft.player.getId()) {
              event.setCanceled(true);
            }
          }

          if (event.getPacket() instanceof ClientboundExplodePacket) {
            event.setCanceled(true);
          }
        }
      };

  public Velocity() {
    super("Velocity", new String[] {"velocity", "velocity-cancel"}, 0xFF0000, ModuleType.MOVEMENT);
    setDescription("Reduces or cancels knockback from attacks.");
    this.listeners.add(packetListener);
  }
}
