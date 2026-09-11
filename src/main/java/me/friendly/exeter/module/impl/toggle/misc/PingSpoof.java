package me.friendly.exeter.module.impl.toggle.misc;

import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;

/** Delays keepalive packets so the tab list shows fake low ping. */
public class PingSpoof extends ToggleableModule {

  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(500, 50, 5000, "Delay");

  private Packet<?> held;
  private long heldAt;
  private boolean flushing;

  public PingSpoof() {
    super("PingSpoof", new String[] {"pingspoof", "ping-spoof"}, 0x00FFFF,
        ModuleType.MISCELLANEOUS);
    setDescription("Spoofs your ping with delayed keepalives.");
    offerProperties(delay);
    this.listeners.add(
        new Listener<TickEvent>("pingspoof_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            PingSpoof.this.onTick();
          }
        });
    this.listeners.add(
        new Listener<PacketEvent>("pingspoof_packet") {
          @Override
          public void call(PacketEvent event) {
            PingSpoof.this.onPacket(event);
          }
        });
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    flush();
  }

  private void onTick() {
    if (held != null && System.currentTimeMillis() - heldAt >= delay.getValue()) {
      flush();
    }
    setTag("PingSpoof [" + delay.getValue() + "ms]");
  }

  private void onPacket(PacketEvent event) {
    if (!(event.getPacket() instanceof ServerboundKeepAlivePacket)) return;
    // Own flush resend must pass through, or the keepalive never leaves.
    if (flushing) return;
    event.setCanceled(true);
    // Keep only the latest: older keepalives are superseded.
    held = event.getPacket();
    heldAt = System.currentTimeMillis();
  }

  private void flush() {
    if (held != null && minecraft.getConnection() != null) {
      flushing = true;
      minecraft.getConnection().send(held);
      flushing = false;
    }
    held = null;
  }
}
