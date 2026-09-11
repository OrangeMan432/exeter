package me.larp.client.module.impl.toggle.misc;

import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.PacketEvent;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;

/** Meteor-pattern AutoLog: disconnects on low HP, totem pops, or nearby players. */
public class AutoLog extends ToggleableModule {

  private final Property<Boolean> onHealth =
      new Property<Boolean>(true, "On Health");
  private final NumberProperty<Double> health =
      new NumberProperty<Double>(8.0, 0.0, 20.0, "Health");
  private final Property<Boolean> onPop =
      new Property<Boolean>(false, "On Pop");
  private final NumberProperty<Integer> pops =
      new NumberProperty<Integer>(1, 1, 5, "Pops");
  private final Property<Boolean> onPlayer =
      new Property<Boolean>(false, "On Player");
  private final NumberProperty<Double> playerRange =
      new NumberProperty<Double>(50.0, 5.0, 200.0, "Player Range");

  private int popCount;

  public AutoLog() {
    super("AutoLog", new String[] {"autolog", "auto-log"}, 0x00FFFF, ModuleType.MISCELLANEOUS);
    setDescription("Disconnects you before you die.");
    offerProperties(onHealth, health, onPop, pops, onPlayer, playerRange);
    this.listeners.add(
        new Listener<TickEvent>("autolog_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            AutoLog.this.onTick();
          }
        });
    this.listeners.add(
        new Listener<PacketEvent>("autolog_packet") {
          @Override
          public void call(PacketEvent event) {
            AutoLog.this.onPacket(event);
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    popCount = 0;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    if (minecraft.player.isDeadOrDying()) return;

    if (onHealth.getValue()
        && minecraft.player.getHealth() + minecraft.player.getAbsorptionAmount()
            <= health.getValue().floatValue()) {
      log("Low HP");
      return;
    }

    if (onPop.getValue() && popCount >= pops.getValue()) {
      log("Totem pops");
      return;
    }

    if (onPlayer.getValue()) {
      for (Entity entity : minecraft.level.players()) {
        if (entity == minecraft.player || !entity.isAlive()) continue;
        if (minecraft.player.distanceTo(entity) <= playerRange.getValue()) {
          log("Player in range");
          return;
        }
      }
    }
  }

  private void onPacket(PacketEvent event) {
    if (!(event.getPacket() instanceof ClientboundEntityEventPacket packet)) return;
    if (minecraft.level == null || minecraft.player == null) return;
    // 35 = totem pop animation (AutoPot already relies on this id).
    if (packet.getEventId() != 35) return;
    Entity target = packet.getEntity(minecraft.level);
    if (target == minecraft.player) {
      popCount++;
    }
  }

  private void log(String reason) {
    if (minecraft.getConnection() != null) {
      minecraft.getConnection().getConnection().disconnect(Component.literal("AutoLog: " + reason));
    }
    setRunning(false);
  }
}
