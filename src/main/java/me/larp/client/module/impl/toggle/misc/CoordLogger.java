package me.larp.client.module.impl.toggle.misc;

import me.larp.api.event.Listener;
import me.larp.client.events.PacketEvent;
import me.larp.client.logging.Logger;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Meteor-Rejects-pattern CoordLogger: reports player teleports and global world events
 * (wither spawns, end portals, dragon kills) with coordinates.
 */
public class CoordLogger extends ToggleableModule {

  private final NumberProperty<Double> minDistance =
      new NumberProperty<Double>(10.0, 5.0, 100.0, "Min Distance");
  private final Property<Boolean> players = new Property<Boolean>(true, "Players");
  private final Property<Boolean> wolves = new Property<Boolean>(false, "Wolves");
  private final Property<Boolean> withers = new Property<Boolean>(false, "Withers");
  private final Property<Boolean> endPortals = new Property<Boolean>(false, "End Portals");
  private final Property<Boolean> enderDragons = new Property<Boolean>(false, "Ender Dragons");
  private final Property<Boolean> otherEvents = new Property<Boolean>(false, "Other Events");

  public CoordLogger() {
    super("CoordLogger", new String[] {"coordlogger", "coords"}, 0x888888, ModuleType.MISCELLANEOUS);
    setDescription("Logs teleports and world events with coords.");
    offerProperties(minDistance, players, wolves, withers, endPortals, enderDragons, otherEvents);
    this.listeners.add(
        new Listener<PacketEvent>("coordlogger_packet") {
          @Override
          public void call(PacketEvent event) {
            if (event.getPacket() instanceof ClientboundTeleportEntityPacket
                || event.getPacket() instanceof ClientboundLevelEventPacket) {
              var packet = event.getPacket();
              // Inbound packets arrive on the netty thread; touch game state on the client thread.
              minecraft.execute(() -> CoordLogger.this.onPacket(packet));
            }
          }
        });
  }

  private void onPacket(net.minecraft.network.protocol.Packet<?> packet) {
    if (minecraft.level == null || minecraft.player == null) return;
    if (packet instanceof ClientboundTeleportEntityPacket teleport) {
      onTeleport(teleport);
    } else if (packet instanceof ClientboundLevelEventPacket worldEvent) {
      onWorldEvent(worldEvent);
    }
  }

  private void onTeleport(ClientboundTeleportEntityPacket packet) {
    Entity entity = minecraft.level.getEntity(packet.id());
    if (entity == null) return;
    Vec3 packetPos = packet.change().position();
    Vec3 current = new Vec3(entity.getX(), entity.getY(), entity.getZ());
    if (current.distanceTo(packetPos) < minDistance.getValue()) return;
    if (entity instanceof Player && players.getValue()) {
      log("Player '" + entity.getScoreboardName() + "' teleported to ", packetPos);
    } else if (entity instanceof Wolf && wolves.getValue()) {
      log("Wolf teleported to ", packetPos);
    }
  }

  private void onWorldEvent(ClientboundLevelEventPacket packet) {
    if (!packet.isGlobalEvent()) return;
    BlockPos pos = packet.getPos();
    Vec3 eventPos = new Vec3(pos.getX(), pos.getY(), pos.getZ());
    if (minecraft.player.position().distanceTo(eventPos) <= minDistance.getValue()) return;
    switch (packet.getType()) {
      case 1023 -> {
        if (withers.getValue()) log("Wither spawned at ", eventPos);
      }
      case 1038 -> {
        if (endPortals.getValue()) log("End portal opened at ", eventPos);
      }
      case 1028 -> {
        if (enderDragons.getValue()) log("Ender dragon killed at ", eventPos);
      }
      default -> {
        if (otherEvents.getValue()) log("Global event " + packet.getType() + " at ", eventPos);
      }
    }
  }

  private static void log(String message, Vec3 coords) {
    Logger.getLogger()
        .printToChat(
            message
                + (int) coords.x
                + ", "
                + (int) coords.y
                + ", "
                + (int) coords.z
                + ".");
  }
}
