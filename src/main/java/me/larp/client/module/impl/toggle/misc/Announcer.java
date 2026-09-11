package me.larp.client.module.impl.toggle.misc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.PacketEvent;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.EnumProperty;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Announces server events in public chat: totem pops, deaths, joins, leaves.
 * Cooldown-gated so you don't catch a mute.
 */
public class Announcer extends ToggleableModule {

  public enum Style {
    CLEAN,
    BM
  }

  private final Property<Boolean> pops = new Property<Boolean>(true, "Totem Pops");
  private final Property<Boolean> kills = new Property<Boolean>(true, "Kills");
  private final Property<Boolean> joins = new Property<Boolean>(true, "Joins");
  private final Property<Boolean> leaves = new Property<Boolean>(true, "Leaves");
  private final EnumProperty<Style> style = new EnumProperty<Style>(Style.CLEAN, "Style");
  private final NumberProperty<Integer> cooldown =
      new NumberProperty<Integer>(5, 1, 60, "Cooldown");

  private final Map<String, Integer> popCounts = new HashMap<>();
  private final Set<String> known = new HashSet<>();
  private long lastMessage;

  public Announcer() {
    super("Announcer", new String[] {"announcer", "announce"}, 0x00FFFF,
        ModuleType.MISCELLANEOUS);
    setDescription("Announces pops, kills, joins and leaves.");
    offerProperties(pops, kills, joins, leaves, style, cooldown);
    this.listeners.add(
        new Listener<TickEvent>("announcer_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            Announcer.this.onTick();
          }
        });
    this.listeners.add(
        new Listener<PacketEvent>("announcer_packet") {
          @Override
          public void call(PacketEvent event) {
            Announcer.this.onPacket(event);
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    popCounts.clear();
    known.clear();
    lastMessage = 0;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null) return;
    Set<String> now = new HashSet<>();
    for (Entity entity : minecraft.level.players()) {
      if (!(entity instanceof Player player)) continue;
      if (entity == minecraft.player) continue;
      String name = player.getGameProfile().name();
      now.add(name);
      // Death: known alive player now dead.
      if (kills.getValue() && known.contains(name) && player.isDeadOrDying()) {
        say(style.getValue() == Style.BM
            ? name + " got packed, Larp Client on top"
            : "gg " + name);
      }
    }
    if (joins.getValue()) {
      for (String name : now) {
        if (!known.contains(name)) {
          say(style.getValue() == Style.BM
              ? "welcome to the graveyard " + name
              : "welcome " + name);
        }
      }
    }
    if (leaves.getValue()) {
      for (String name : known) {
        if (!now.contains(name)) {
          say("bye " + name);
        }
      }
    }
    known.clear();
    known.addAll(now);
    // Forget pop counts of players long gone.
    popCounts.keySet().retainAll(now);
  }

  private void onPacket(PacketEvent event) {
    if (!pops.getValue()) return;
    if (!(event.getPacket() instanceof ClientboundEntityEventPacket packet)) return;
    if (packet.getEventId() != 35) return;
    if (minecraft.level == null || minecraft.player == null) return;
    Entity target = packet.getEntity(minecraft.level);
    if (!(target instanceof Player player) || target == minecraft.player) return;
    String name = player.getGameProfile().name();
    int count = popCounts.getOrDefault(name, 0) + 1;
    popCounts.put(name, count);
    say(style.getValue() == Style.BM
        ? name + " popped " + count + " totem" + (count == 1 ? "" : "s") + ", keep feeding"
        : name + " popped " + count + " totem" + (count == 1 ? "" : "s"));
  }

  private void say(String message) {
    if (minecraft.getConnection() == null) return;
    long now = System.currentTimeMillis();
    if (now - lastMessage < cooldown.getValue() * 1000L) return;
    lastMessage = now;
    minecraft.getConnection().sendChat(message);
  }
}
