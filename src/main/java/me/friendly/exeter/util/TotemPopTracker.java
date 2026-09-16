package me.friendly.exeter.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class TotemPopTracker {

  private static TotemPopTracker instance;

  private final Map<UUID, Integer> pops = new ConcurrentHashMap<>();

  private final Listener<PacketEvent> packetListener =
      new Listener<PacketEvent>("totem_pop_tracker") {
        @Override
        public void call(PacketEvent event) {
          if (!(event.getPacket() instanceof ClientboundEntityEventPacket packet)) return;
          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null) return;
          Entity entity = packet.getEntity(mc.level);
          if (!(entity instanceof Player player)) return;
          byte id = packet.getEventId();
          if (id == 35) {
            pops.merge(player.getUUID(), 1, Integer::sum);
          } else if (id == 3) {
            pops.put(player.getUUID(), 0);
          }
        }
      };

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("totem_pop_tracker_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null || mc.player == null) return;
          for (Player player : mc.level.players()) {
            if (player == null) continue;
            if (player.isDeadOrDying() || player.getHealth() <= 0.0f) {
              UUID uuid = player.getUUID();
              if (pops.getOrDefault(uuid, 0) != 0) {
                pops.put(uuid, 0);
              }
            }
          }
        }
      };

  private TotemPopTracker() {
    Exeter.getInstance().getEventManager().register(packetListener);
    Exeter.getInstance().getEventManager().register(tickListener);
  }

  public static TotemPopTracker getInstance() {
    if (instance == null) {
      instance = new TotemPopTracker();
    }
    return instance;
  }

  public int getPops(UUID uuid) {
    return pops.getOrDefault(uuid, 0);
  }

  public int getPops(Player player) {
    return getPops(player.getUUID());
  }

  public void setPops(UUID uuid, int count) {
    pops.put(uuid, count);
  }

  public void clear() {
    pops.clear();
  }

  public Map<UUID, Integer> getMap() {
    return pops;
  }
}
