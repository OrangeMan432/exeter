package me.friendly.exeter.module.impl.toggle.movement;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * FakeLag: chokes movement packets then flushes them, teleporting you on the
 * server side. Flushes automatically so you never hard-desync.
 */
public class FakeLag extends ToggleableModule {

  private final NumberProperty<Integer> packets =
      new NumberProperty<Integer>(20, 5, 100, "Packets");
  private final NumberProperty<Integer> flushDelay =
      new NumberProperty<Integer>(40, 10, 200, "Flush Delay");

  private final List<Packet<?>> held = new ArrayList<>();
  private int tickCounter;

  public FakeLag() {
    super("FakeLag", new String[] {"fakelag", "fake-lag"}, 0x00FF00, ModuleType.MOVEMENT);
    setDescription("Chokes movement packets for lag teleportation.");
    offerProperties(packets, flushDelay);
    this.listeners.add(
        new Listener<TickEvent>("fakelag_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            FakeLag.this.onTick();
          }
        });
    this.listeners.add(
        new Listener<PacketEvent>("fakelag_packet") {
          @Override
          public void call(PacketEvent event) {
            FakeLag.this.onPacket(event);
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    held.clear();
    tickCounter = 0;
  }

  @Override
  protected void onDisable() {
    super.onDisable();
    flush();
  }

  private void onTick() {
    if (minecraft.player == null) return;
    if (tickCounter++ >= flushDelay.getValue()) {
      tickCounter = 0;
      flush();
    }
    setTag("FakeLag [" + held.size() + "]");
  }

  private void onPacket(PacketEvent event) {
    if (!(event.getPacket() instanceof ServerboundMovePlayerPacket)) return;
    if (minecraft.player == null) return;
    if (held.size() >= packets.getValue()) {
      flush();
      return;
    }
    event.setCanceled(true);
    held.add(event.getPacket());
  }

  private void flush() {
    if (minecraft.getConnection() == null) {
      held.clear();
      return;
    }
    for (Packet<?> packet : held) {
      minecraft.getConnection().send(packet);
    }
    held.clear();
  }
}
