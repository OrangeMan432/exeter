package me.friendly.exeter.module.impl.toggle.combat;

import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class KillAura extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(4.0, 1.0, 6.0, "Range", "range");

  private final Listener<TickEvent> tickListener =
      new Listener<TickEvent>("killaura_tick") {
        @Override
        public void call(TickEvent event) {
          if (event.getStage() != Stage.PRE) return;
          KillAura.this.onTick();
        }
      };

  public KillAura() {
    super("KillAura", new String[] {"killaura", "aura"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Attacks the nearest enemy in range.");
    offerProperties(range);
    this.listeners.add(tickListener);
  }

  private void onTick() {
    if (minecraft() == null || minecraft().player == null || minecraft().world == null) {
      return;
    }
    PlayerEntity target = findTarget();
    if (target == null) {
      return;
    }
    double dx = target.x - minecraft().player.x;
    double dz = target.z - minecraft().player.z;
    minecraft().player.yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    minecraft().interactionManager.attackEntity(minecraft().player, target);
  }

  private PlayerEntity findTarget() {
    List<PlayerEntity> players = PlayerUtil.players();
    PlayerEntity closest = null;
    double closestDist = range.getValue().doubleValue();
    for (int i = 0; i < players.size(); i++) {
      PlayerEntity player = players.get(i);
      if (player == minecraft().player) continue;
      if (player instanceof LivingEntity && ((LivingEntity) player).health <= 0) continue;
      if (!Exeter.getInstance().getFriendManager().isTargetable(player.name)) {
        continue;
      }
      double dist = PlayerUtil.distanceTo(player);
      if (dist < closestDist) {
        closestDist = dist;
        closest = player;
      }
    }
    return closest;
  }
}
