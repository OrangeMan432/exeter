package me.larp.client.module.impl.toggle.combat;

import java.util.Comparator;
import me.larp.api.event.Listener;
import me.larp.api.event.Stage;
import me.larp.client.events.TickEvent;
import me.larp.client.module.ModuleType;
import me.larp.client.module.ToggleableModule;
import me.larp.client.properties.NumberProperty;
import me.larp.client.properties.Property;
import me.larp.client.util.PlayerUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;

public class KillAura extends ToggleableModule {

  private final NumberProperty<Double> range =
      new NumberProperty<Double>(4.5, 1.0, 6.0, "Range");
  private final NumberProperty<Double> wallsRange =
      new NumberProperty<Double>(3.5, 0.0, 6.0, "Walls Range");
  private final NumberProperty<Double> cooldown =
      new NumberProperty<Double>(1.0, 0.0, 1.0, "Cooldown");
  private final NumberProperty<Integer> delay =
      new NumberProperty<Integer>(1, 0, 20, "Delay");
  private final Property<Boolean> rotate = new Property<Boolean>(true, "Rotate");
  private final Property<Boolean> swingHand = new Property<Boolean>(true, "Swing Hand");
  private final Property<Boolean> targetPlayers = new Property<Boolean>(true, "Players");
  private final Property<Boolean> targetHostiles = new Property<Boolean>(true, "Hostiles");
  private final Property<Boolean> autoSwitch = new Property<Boolean>(true, "Auto Switch");
  private final Property<Boolean> onlyWeapon = new Property<Boolean>(false, "Only Weapon");
  private final Property<Boolean> antiWeakness =
      new Property<Boolean>(true, "Anti Weakness");

  private int tickCounter = -100;

  public KillAura() {
    super("KillAura", new String[] {"killaura", "kill-aura"}, 0xFF0000, ModuleType.COMBAT);
    setDescription("Attacks entities in range automatically.");
    offerProperties(
        range,
        wallsRange,
        cooldown,
        delay,
        rotate,
        swingHand,
        targetPlayers,
        targetHostiles,
        autoSwitch,
        onlyWeapon,
        antiWeakness);
    this.listeners.add(
        new Listener<TickEvent>("killaura_tick") {
          @Override
          public void call(TickEvent event) {
            if (event.getStage() != Stage.PRE) return;
            KillAura.this.onTick();
          }
        });
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    tickCounter = -100;
  }

  private void onTick() {
    if (minecraft.level == null || minecraft.player == null || minecraft.gameMode == null) return;
    if (minecraft.player.isDeadOrDying()) return;
    tickCounter++;
    if (tickCounter < delay.getValue()) return;

    LivingEntity target = findTarget();
    if (target == null) return;
    // Meteor-pattern: full-charge hits only (0 = Future-style spam).
    if (minecraft.player.getAttackStrengthScale(0.0F) < cooldown.getValue().floatValue()) return;

    int weaponSlot = findWeaponSlot();
    if (onlyWeapon.getValue() && weaponSlot == -1) return;
    // Future-pattern AntiWeakness: weakness ruins damage unless holding a sword.
    if (antiWeakness.getValue()
        && minecraft.player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)
        && weaponSlot != -1) {
      boolean needSword =
          weaponSlot != minecraft.player.getInventory().getSelectedSlot() || !isSwordHeld();
      if (needSword) {
        PlayerUtil.swapTo(weaponSlot);
        minecraft.gameMode.attack(minecraft.player, target);
        if (swingHand.getValue()) {
          minecraft.player.swing(InteractionHand.MAIN_HAND);
        }
        PlayerUtil.swapBack();
        tickCounter = 0;
        return;
      }
    }
    boolean needSwitch =
        autoSwitch.getValue()
            && weaponSlot != -1
            && weaponSlot != minecraft.player.getInventory().getSelectedSlot();
    if (needSwitch) {
      PlayerUtil.swapTo(weaponSlot);
    }

    float yaw = minecraft.player.getYRot();
    float pitch = minecraft.player.getXRot();
    if (rotate.getValue()) {
      double dx = target.getX() - minecraft.player.getX();
      double dy =
          (target.getY() + target.getBbHeight() * 0.5)
              - (minecraft.player.getY() + minecraft.player.getEyeHeight());
      double dz = target.getZ() - minecraft.player.getZ();
      double dist = Math.sqrt(dx * dx + dz * dz);
      minecraft.player.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
      minecraft.player.setXRot((float) (-Math.toDegrees(Math.atan2(dy, dist))));
    }

    minecraft.gameMode.attack(minecraft.player, target);
    if (swingHand.getValue()) {
      minecraft.player.swing(InteractionHand.MAIN_HAND);
    }

    if (rotate.getValue()) {
      PlayerUtil.restoreRotation(yaw, pitch);
    }
    if (needSwitch) {
      PlayerUtil.swapBack();
    }
    tickCounter = 0;
  }

  private LivingEntity findTarget() {
    double rangeSq = range.getValue() * range.getValue();
    double wallsSq = wallsRange.getValue() * wallsRange.getValue();
    return java.util.stream.StreamSupport.stream(
            minecraft.level.entitiesForRendering().spliterator(), false)
        .filter(e -> e instanceof LivingEntity)
        .filter(e -> e != minecraft.player)
        .filter(Entity::isAlive)
        .filter(
            e -> {
              if (e instanceof Player) return targetPlayers.getValue();
              if (e instanceof Enemy) return targetHostiles.getValue();
              return false;
            })
        .filter(e -> minecraft.player.distanceToSqr(e) <= rangeSq)
        .filter(
            e -> {
              // Through walls only at reduced walls-range (Meteor-pattern).
              if (minecraft.player.hasLineOfSight(e)) return true;
              return minecraft.player.distanceToSqr(e) <= wallsSq;
            })
        .min(Comparator.comparingDouble(minecraft.player::distanceTo))
        .map(e -> (LivingEntity) e)
        .orElse(null);
  }

  private boolean isSwordHeld() {
    ItemStack held = minecraft.player.getMainHandItem();
    if (held.isEmpty()) return false;
    return BuiltInRegistries.ITEM.getKey(held.getItem()).getPath().endsWith("_sword");
  }

  private int findWeaponSlot() {
    int sword = -1;
    int axe = -1;
    for (int i = 0; i < 9; i++) {
      ItemStack stack = minecraft.player.getInventory().getItem(i);
      if (stack.isEmpty()) continue;
      // 26.2 has no SwordItem class (weapons are data-driven), so match by registry name.
      String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
      if (path.endsWith("_sword") && sword == -1) {
        sword = i;
      } else if ((stack.getItem() instanceof AxeItem || path.endsWith("_axe")) && axe == -1) {
        axe = i;
      }
    }
    if (sword != -1) return sword;
    return axe;
  }
}
