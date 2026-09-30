package me.friendly.exeter.module.impl.toggle.combat;

import java.util.ArrayList;
import java.util.List;
import me.friendly.api.event.Listener;
import me.friendly.api.event.Stage;
import me.friendly.exeter.core.Exeter;
import me.friendly.exeter.events.TickEvent;
import me.friendly.exeter.module.ModuleType;
import me.friendly.exeter.module.ToggleableModule;
import me.friendly.exeter.properties.NumberProperty;
import me.friendly.exeter.properties.Property;
import me.friendly.exeter.util.PlayerUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class KillAura extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(4.0, 1.0, 6.0, "Range", "range");
  private final Property<Boolean> players = new Property<Boolean>(true, "Players", "players");
  private final Property<Boolean> passive = new Property<Boolean>(false, "Passive", "passive");
  private final Property<Boolean> hostile = new Property<Boolean>(true, "Hostile", "hostile");

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
    offerProperties(range, players, passive, hostile);
    this.listeners.add(tickListener);
  }

  private void onTick() {
    if (minecraft() == null || minecraft().player == null || minecraft().world == null) {
      return;
    }
    Entity target = findTarget();
    if (target == null) {
      return;
    }
    double dx = target.x - minecraft().player.x;
    double dz = target.z - minecraft().player.z;
    minecraft().player.yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    minecraft().interactionManager.attackEntity(minecraft().player, target);
  }

  private Entity findTarget() {
    boolean wantPlayers = players.getValue().booleanValue();
    boolean wantPassive = passive.getValue().booleanValue();
    boolean wantHostile = hostile.getValue().booleanValue();
    Entity closest = null;
    double closestDist = range.getValue().doubleValue();
    for (PlayerEntity player : PlayerUtil.players()) {
      if (player == minecraft().player) continue;
      if (!wantPlayers) continue;
      if (player.health <= 0) continue;
      if (!Exeter.getInstance().getFriendManager().isTargetable(player.name)) {
        continue;
      }
      double dist = PlayerUtil.distanceTo(player);
      if (dist < closestDist) {
        closestDist = dist;
        closest = player;
      }
    }
    if (wantPassive || wantHostile) {
      for (LivingEntity entity : livingEntities()) {
        if (entity instanceof PlayerEntity) continue;
        if (entity.health <= 0) continue;
        if (isHostile(entity)) {
          if (!wantHostile) continue;
        } else if (isPassive(entity)) {
          if (!wantPassive) continue;
        } else {
          continue;
        }
        double dist = PlayerUtil.distanceTo(entity);
        if (dist < closestDist) {
          closestDist = dist;
          closest = entity;
        }
      }
    }
    return closest;
  }

  /**
   * Barn names almost no mob classes, so this is decoded from the entity list instead: hostile
   * mobs live under class_146 (zombie/skeleton/spider/creeper/giant family) plus slime
   * (class_451) and ghast (class_364); passives live under class_258 (pig/sheep/cow/chicken/wolf
   * family) plus squid (WaterCreatureEntity).
   */
  private boolean isHostile(LivingEntity entity) {
    return entity instanceof net.minecraft.class_146
        || entity instanceof net.minecraft.class_451
        || entity instanceof net.minecraft.class_364;
  }

  private boolean isPassive(LivingEntity entity) {
    return entity instanceof net.minecraft.class_258
        || entity instanceof net.minecraft.entity.mob.WaterCreatureEntity;
  }

  private List<LivingEntity> livingEntities() {
    List<LivingEntity> result = new ArrayList<LivingEntity>();
    if (minecraft() == null || minecraft().world == null) {
      return result;
    }
    net.minecraft.world.World world = minecraft().world;
    java.util.List[] lists =
        new java.util.List[] {world.field_198, world.field_199, world.field_200, world.field_201};
    for (int i = 0; i < lists.length; i++) {
      java.util.List list = lists[i];
      if (list == null) continue;
      Object[] copy = list.toArray();
      for (int j = 0; j < copy.length; j++) {
        if (copy[j] instanceof LivingEntity) {
          result.add((LivingEntity) copy[j]);
        }
      }
    }
    return result;
  }
}
