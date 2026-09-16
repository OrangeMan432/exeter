package me.friendly.exeter.module.impl.toggle.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.events.PacketEvent;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.NotificationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class Notifier extends ToggleableModule {

  public final Property<Boolean> visualRange = new Property<>(true, "VisualRange", "vr");
  public final Property<Boolean> totemNotify = new Property<>(true, "TotemPops", "totems");
  public final Property<Boolean> armorLow = new Property<>(true, "ArmorLow", "armorlow");
  public final Property<Boolean> armorBreak = new Property<>(true, "ArmorBreak", "armorbreak");
  public final Property<Boolean> teleports = new Property<>(true, "Teleports", "tp");
  public final Property<Boolean> pearls = new Property<>(true, "Pearls", "pearl");

  // visual range
  private final Set<UUID> knownPlayers = new HashSet<>();
  private final Map<UUID, String> knownNames = new HashMap<>();

  // totem pops
  private final Map<UUID, Integer> totemPops = new HashMap<>();

  // armor low warning dedup
  private final Set<String> warnedArmor = new HashSet<>();
  private final Map<UUID, ItemStack[]> prevArmor = new HashMap<>();

  // teleport
  private final Map<UUID, Vec3> prevPos = new HashMap<>();

  // pearls
  private final Set<Integer> knownPearls = new HashSet<>();
  private final Map<UUID, Long> lastPearlThrow = new HashMap<>();

  private final Listener<PacketEvent> packetListener =
      new Listener<PacketEvent>("notifier_packet") {
        @Override
        public void call(PacketEvent event) {
          if (!(event.getPacket() instanceof ClientboundEntityEventPacket pkt)) return;
          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null) return;
          Entity ent = pkt.getEntity(mc.level);
          if (!(ent instanceof Player player)) return;
          byte id = pkt.getEventId();
          if (id == 35) {
            UUID uuid = player.getUUID();
            int count = totemPops.getOrDefault(uuid, 0) + 1;
            totemPops.put(uuid, count);
            if (totemNotify.getValue()) {
              String name = player.getName().getString();
              boolean self = player == mc.player;
              String text = name + " popped " + count + " totem" + (count == 1 ? "" : "s")
                  + (self ? " (you)" : "");
              NotificationManager.push(text, "totem_of_undying");
            }
          } else if (id == 3) {
            totemPops.put(player.getUUID(), 0);
          }
        }
      };

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("notifier_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          Minecraft mc = Minecraft.getInstance();
          if (mc.level == null || mc.player == null) return;

          // visual range
          Set<UUID> current = new HashSet<>();
          for (Player p : mc.level.players()) {
            if (p == null) continue;
            UUID uuid = p.getUUID();
            current.add(uuid);
            knownNames.put(uuid, p.getName().getString());
            if (visualRange.getValue() && !knownPlayers.contains(uuid) && p != mc.player) {
              NotificationManager.push(p.getName().getString() + " entered visual range", "eye");
            }
          }
          for (UUID uuid : new HashSet<>(knownPlayers)) {
            if (!current.contains(uuid)) {
              if (visualRange.getValue()) {
                String name = knownNames.getOrDefault(uuid, uuid.toString().substring(0, 8));
                NotificationManager.push(name + " left visual range", "eye_closed");
              }
              knownNames.remove(uuid);
            }
          }
          knownPlayers.clear();
          knownPlayers.addAll(current);

          // armor checks + teleport
          for (Player p : mc.level.players()) {
            if (p == null) continue;
            if (armorLow.getValue() || armorBreak.getValue()) checkArmor(p);
            if (teleports.getValue()) checkTeleport(p);
          }

          // pearls
          if (pearls.getValue()) checkPearls();
        }
      };

  public Notifier() {
    super("Notifier", new String[] {"notifier", "notify", "notif"}, 0xFFAA55, ModuleType.CLIENT);
    setDescription("Sends notifications for game events.");
    offerProperties(visualRange, totemNotify, armorLow, armorBreak, teleports, pearls);
    listeners.add(packetListener);
    listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    knownPlayers.clear();
    totemPops.clear();
    warnedArmor.clear();
    prevArmor.clear();
    prevPos.clear();
    knownPearls.clear();
    lastPearlThrow.clear();
    Minecraft mc = Minecraft.getInstance();
    if (mc.level != null) {
      for (Player p : mc.level.players()) {
        knownPlayers.add(p.getUUID());
        prevPos.put(p.getUUID(), p.position());
        saveArmor(p);
      }
      for (Entity e : mc.level.entitiesForRendering()) {
        if (e instanceof ThrownEnderpearl) knownPearls.add(e.getId());
      }
    }
  }

  private void saveArmor(Player p) {
    ItemStack[] arr = new ItemStack[4];
    arr[0] = p.getItemBySlot(EquipmentSlot.HEAD).copy();
    arr[1] = p.getItemBySlot(EquipmentSlot.CHEST).copy();
    arr[2] = p.getItemBySlot(EquipmentSlot.LEGS).copy();
    arr[3] = p.getItemBySlot(EquipmentSlot.FEET).copy();
    prevArmor.put(p.getUUID(), arr);
  }

  private void checkArmor(Player p) {
    UUID uuid = p.getUUID();
    String name = p.getName().getString();
    ItemStack[] prev = prevArmor.get(uuid);
    ItemStack[] cur = new ItemStack[] {
        p.getItemBySlot(EquipmentSlot.HEAD),
        p.getItemBySlot(EquipmentSlot.CHEST),
        p.getItemBySlot(EquipmentSlot.LEGS),
        p.getItemBySlot(EquipmentSlot.FEET)
    };
    String[] slotNames = {"Helmet", "Chestplate", "Leggings", "Boots"};

    for (int i = 0; i < 4; i++) {
      ItemStack stack = cur[i];
      String key = uuid + ":" + i;

      // break detection: had item, now empty/air
      if (prev != null && !prev[i].isEmpty() && stack.isEmpty()) {
        NotificationManager.push(name + " " + slotNames[i] + " broke", "cross");
        warnedArmor.remove(key);
        continue;
      }

      if (stack.isEmpty() || !stack.isDamageableItem()) {
        warnedArmor.remove(key);
        continue;
      }
      int max = stack.getMaxDamage();
      int dmg = stack.getDamageValue();
      int remaining = max - dmg;
      float pct = max > 0 ? (float) remaining / max * 100f : 100f;

      if (pct < 15f) {
        if (!warnedArmor.contains(key)) {
          warnedArmor.add(key);
          String text = String.format("%s %s low (%d%%)", name, slotNames[i], (int) pct);
          NotificationManager.push(text, "exclamation");
        }
      } else {
        warnedArmor.remove(key);
      }

      // also detect sudden break via damage exceeding max (should be caught above)
      if (remaining <= 0 && prev != null && !prev[i].isEmpty()) {
        NotificationManager.push(name + " " + slotNames[i] + " broke", "cross");
      }
    }
    saveArmor(p);
  }

  private void checkTeleport(Player p) {
    UUID uuid = p.getUUID();
    Vec3 cur = p.position();
    Vec3 prev = prevPos.get(uuid);
    if (prev != null) {
      double dx = cur.x - prev.x;
      double dy = cur.y - prev.y;
      double dz = cur.z - prev.z;
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      boolean pearlRecent = System.currentTimeMillis() - lastPearlThrow.getOrDefault(uuid, 0L) < 10000;
      double threshold = pearlRecent ? 3.0 : 8.0;
      // ignore small movement and respawn at world spawn; threshold 8 blocks (3 if pearl) in one tick ~ teleport
      if (dist > threshold && dist < 10000) {
        String from = String.format("%.1f, %.1f, %.1f", prev.x, prev.y, prev.z);
        String to = String.format("%.1f, %.1f, %.1f", cur.x, cur.y, cur.z);
        String text = p.getName().getString() + " teleported " + from + " -> " + to;
        NotificationManager.push(text, "ender_pearl");
      }
    }
    prevPos.put(uuid, cur);
  }

  private void checkPearls() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return;
    for (Entity e : mc.level.entitiesForRendering()) {
      if (!(e instanceof ThrownEnderpearl pearl)) continue;
      int id = pearl.getId();
      if (knownPearls.contains(id)) continue;
      knownPearls.add(id);
      Entity owner = pearl.getOwner();
      if (owner instanceof Player player) {
        lastPearlThrow.put(player.getUUID(), System.currentTimeMillis());
        Vec3 vel = pearl.getDeltaMovement();
        String dir;
        if (Math.abs(vel.x) > Math.abs(vel.z)) {
          dir = vel.x > 0 ? "east" : "west";
        } else {
          dir = vel.z > 0 ? "south" : "north";
        }
        String text = player.getName().getString() + " threw pearl " + dir;
        NotificationManager.push(text, "ender_pearl");
      }
    }
    // cleanup removed pearls
    knownPearls.removeIf(id -> mc.level.getEntity(id) == null);
  }
}
