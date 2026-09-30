package me.friendly.exeter.module.impl.toggle.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.beta.mixin.MinecraftAccessor;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.NotificationManager;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class Notifier extends ToggleableModule {

  public final Property<Boolean> visualRange = new Property<Boolean>(true, "VisualRange", "vr");
  public final Property<Boolean> armorLow = new Property<Boolean>(true, "ArmorLow", "armorlow");
  public final Property<Boolean> armorBreak = new Property<Boolean>(true, "ArmorBreak", "armorbreak");
  public final Property<Boolean> teleports = new Property<Boolean>(true, "Teleports", "tp");

  private final Set<String> knownPlayers = new HashSet<String>();
  private final Map<String, String> knownNames = new HashMap<String, String>();
  private final Set<String> warnedArmor = new HashSet<String>();
  private final Map<String, Vec3d> prevPos = new HashMap<String, Vec3d>();

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("notifier_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          Notifier.this.onTick();
        }
      };

  public Notifier() {
    super("Notifier", new String[] {"notifier", "notify", "notif"}, 0xFFAA55, ModuleType.CLIENT);
    setDescription("Sends notifications for game events.");
    offerProperties(visualRange, armorLow, armorBreak, teleports);
    this.listeners.add(tickListener);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    knownPlayers.clear();
    warnedArmor.clear();
    prevPos.clear();
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc != null && mc.player != null) {
      for (PlayerEntity p : PlayerUtil.players()) {
        knownPlayers.add(p.name);
        prevPos.put(p.name, Vec3d.create(p.x, p.y, p.z));
      }
    }
  }

  private void onTick() {
    Minecraft mc = MinecraftAccessor.getMinecraft();
    if (mc == null || mc.player == null) return;

    Set<String> current = new HashSet<String>();
    for (PlayerEntity p : PlayerUtil.players()) {
      if (p == null) continue;
      current.add(p.name);
      knownNames.put(p.name, p.name);
      if (visualRange.getValue().booleanValue() && !knownPlayers.contains(p.name)
          && p != mc.player) {
        NotificationManager.push(p.name + " entered visual range", "eye");
      }
      if (armorLow.getValue().booleanValue() || armorBreak.getValue().booleanValue()) {
        checkArmor(p);
      }
      if (teleports.getValue().booleanValue()) {
        checkTeleport(p);
      }
    }
    for (String name : new HashSet<String>(knownPlayers)) {
      if (!current.contains(name)) {
        if (visualRange.getValue().booleanValue()) {
          NotificationManager.push(name + " left visual range", "eye_closed");
        }
        knownNames.remove(name);
      }
    }
    knownPlayers.clear();
    knownPlayers.addAll(current);
  }

  private void checkArmor(PlayerEntity p) {
    if (p.inventory == null || p.inventory.armor == null) return;
    String[] slotNames = {"Helmet", "Chestplate", "Leggings", "Boots"};
    for (int i = 0; i < 4 && i < p.inventory.armor.length; i++) {
      net.minecraft.item.ItemStack stack = p.inventory.armor[i];
      String key = p.name + ":" + i;
      if (stack == null) {
        warnedArmor.remove(key);
        continue;
      }
      if (!stack.isDamageable()) {
        warnedArmor.remove(key);
        continue;
      }
      int max = stack.getMaxDamage();
      int remaining = max - stack.getDamage();
      float pct = max > 0 ? (float) remaining / max * 100f : 100f;
      if (pct < 15f) {
        if (!warnedArmor.contains(key)) {
          warnedArmor.add(key);
          NotificationManager.push(
              p.name + " " + slotNames[i] + " low (" + (int) pct + "%)", "exclamation");
        }
      } else {
        warnedArmor.remove(key);
      }
    }
  }

  private void checkTeleport(PlayerEntity p) {
    Vec3d cur = Vec3d.create(p.x, p.y, p.z);
    Vec3d prev = prevPos.get(p.name);
    if (prev != null) {
      double dx = cur.x - prev.x;
      double dy = cur.y - prev.y;
      double dz = cur.z - prev.z;
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (dist > 8.0 && dist < 10000) {
        NotificationManager.push(p.name + " teleported", "ender_pearl");
      }
    }
    prevPos.put(p.name, cur);
  }
}
