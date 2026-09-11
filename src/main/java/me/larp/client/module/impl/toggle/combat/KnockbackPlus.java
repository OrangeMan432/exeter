package me.larp.client.module.impl.toggle.combat;

import me.larp.api.event.Listener;
import me.larp.client.core.Larp;
import me.larp.client.events.PacketEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.Property;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Meteor-Rejects-pattern KnockbackPlus: forces sprint state on every attack
 * packet so hits deal sprint knockback. Optionally only with KillAura.
 */
public class KnockbackPlus extends ToggleableModule {

  private final Property<Boolean> onlyKillAura =
      new Property<Boolean>(false, "Only KillAura");

  public KnockbackPlus() {
    super("KnockbackPlus", new String[] {"knockbackplus", "kbplus"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Sprint-resets every attack for maximum knockback.");
    offerProperties(onlyKillAura);
    this.listeners.add(
        new Listener<PacketEvent>("knockbackplus_packet") {
          @Override
          public void call(PacketEvent event) {
            KnockbackPlus.this.onPacket(event);
          }
        });
  }

  private void onPacket(PacketEvent event) {
    if (!(event.getPacket() instanceof ServerboundAttackPacket)) return;
    if (minecraft.player == null || minecraft.getConnection() == null) return;
    if (onlyKillAura.getValue()) {
      KillAura aura = Larp.getInstance().getModuleManager().getModule(KillAura.class);
      if (aura == null || !aura.isRunning()) return;
    }
    // Sprinting attackers deal extra knockback: force the state per swing.
    Entity vehicle = minecraft.player.getVehicle();
    LivingEntity target = findLastTarget();
    if (target != null) {
      minecraft.player.setSprinting(true);
    }
    minecraft.getConnection().send(
        new ServerboundPlayerCommandPacket(
            vehicle != null ? vehicle : minecraft.player,
            ServerboundPlayerCommandPacket.Action.START_SPRINTING));
  }

  private LivingEntity findLastTarget() {
    if (minecraft.level == null || minecraft.player == null) return null;
    LivingEntity best = null;
    double bestDist = 36.0;
    for (Entity e : minecraft.level.entitiesForRendering()) {
      if (!(e instanceof LivingEntity) || e == minecraft.player || !e.isAlive()) continue;
      double d = minecraft.player.distanceToSqr(e);
      if (d < bestDist) {
        bestDist = d;
        best = (LivingEntity) e;
      }
    }
    return best;
  }
}
