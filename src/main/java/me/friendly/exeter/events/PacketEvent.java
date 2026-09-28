package me.friendly.exeter.events;

import me.friendly.api.event.Event;
import net.minecraft.network.protocol.Packet;

public class PacketEvent extends Event {
  private Packet packet;
  private final boolean sending;

  public PacketEvent(Packet packet) {
    this(packet, true);
  }

  public PacketEvent(Packet packet, boolean sending) {
    this.packet = packet;
    this.sending = sending;
  }

  public Packet getPacket() {
    return this.packet;
  }

  public void setPacket(Packet packet) {
    this.packet = packet;
  }

  /** True for client-to-server packets, false for packets read inbound (either side). */
  public boolean isSending() {
    return this.sending;
  }
}
