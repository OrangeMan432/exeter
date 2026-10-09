package me.friendly.exeter.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side mirrors of the vanilla server damage formulas, for predicting explosion and firework
 * damage before acting. Raw damage (before armor, difficulty and enchantments), unless the caller
 * applies its own mitigation on top like PistonCrystal does.
 */
public final class ExplosionUtil {
  private ExplosionUtil() {}

  /**
   * Vanilla TNT/end-crystal style damage: radius is power*2 with AABB-grid exposure and
   * (impact^2+impact)/2*7*radius+1 falloff. Power 4 for TNT and stationary TNT minecarts
   * (MinecartTNT base 4.0, speed bonus only while moving), 6 for end crystals.
   */
  public static float explosionDamage(Level level, Vec3 center, float power, LivingEntity victim) {
    return explosionDamage(
        level, center, power, victim, victim.position(), victim.getBoundingBox());
  }

  /**
   * Same formula with an explicit victim position and box, for scoring predicted positions the
   * entity hasn't reached yet. The victim entity is still passed for the raycast collision context,
   * exactly like vanilla.
   */
  public static float explosionDamage(
      Level level, Vec3 center, float power, LivingEntity victim, Vec3 victimPos, AABB victimBox) {
    float radius = power * 2.0F;
    double dist = victimPos.distanceTo(center) / radius;
    if (dist > 1.0) return 0.0F;
    double exposure = exposure(level, center, victim, victimBox);
    if (exposure <= 0.0) return 0.0F;
    double impact = (1.0 - dist) * exposure;
    if (impact <= 0.0) return 0.0F;
    return (float) ((impact * impact + impact) / 2.0 * 7.0 * radius + 1.0);
  }

  /**
   * Vanilla firework damage: base 5+2 per burst, 5m radius, sqrt falloff. Both line-of-sight rays
   * blocked (or out of range) means vanilla deals 0.
   */
  public static float fireworkDamage(Level level, Vec3 center, int bursts, LivingEntity victim) {
    double distance = center.distanceTo(victim.position());
    if (distance > 5.0) return 0.0F;
    float base = 5.0F + 2.0F * bursts;
    if (base <= 0.0F) return 0.0F;
    if (!hasLineOfSight(level, center, victim)) return 0.0F;
    return (float) (base * Math.sqrt((5.0 - distance) / 5.0));
  }

  /** Burst count from a rocket stack; blanks carry no explosion and deal no damage. */
  public static int fireworkBursts(ItemStack stack) {
    var fireworks = stack.get(DataComponents.FIREWORKS);
    return fireworks == null ? 0 : fireworks.explosions().size();
  }

  /**
   * Client-safe mirror of vanilla {@code getDamageAfterMagicAbsorb} (resistance exact, stats
   * skipped). Protection enchantments resolve their effects against a ServerLevel, which only
   * exists on an integrated server; on remote servers protection is skipped while armor, toughness
   * and resistance still apply.
   */
  private static float magicAbsorb(LivingEntity victim, DamageSource source, float damage) {
    if (source.is(DamageTypeTags.BYPASSES_EFFECTS)) return damage;
    if (victim.hasEffect(MobEffects.RESISTANCE) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
      int scale = (victim.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
      damage = Math.max(damage * (25 - scale) / 25.0F, 0.0F);
    }
    if (source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) return damage;
    var server = Minecraft.getInstance().getSingleplayerServer();
    if (server != null) {
      var serverLevel = server.getLevel(victim.level().dimension());
      if (serverLevel != null) {
        damage =
            CombatRules.getDamageAfterMagicAbsorb(
                damage, EnchantmentHelper.getDamageProtection(serverLevel, victim, source));
      }
    }
    return damage;
  }

  /**
   * 3arthh4ck-style lethal check: the mitigated hit would leave the victim under 1 hp, i.e. pop a
   * totem or kill outright. Health includes absorption, matching {@code EntityUtil.getHealth}; the
   * Max Self Damage gates stay on raw damage.
   */
  public static boolean wouldPop(LivingEntity victim, DamageSource source, float rawDamage) {
    return wouldPopMitigated(victim, mitigatedDamage(victim, source, rawDamage));
  }

  /**
   * Lethal check on an already-mitigated value, for callers whose damage comes back armored
   * (PistonCrystal profiles, mitigated self-damage gates).
   */
  public static boolean wouldPopMitigated(LivingEntity victim, float mitigatedDamage) {
    return mitigatedDamage > victim.getHealth() + victim.getAbsorptionAmount() - 1.0F;
  }

  /** Detached explosion source (explosion type, no entities): armor and enchants apply. */
  public static DamageSource explosionSource(LivingEntity victim) {
    return victim.damageSources().explosion(null, null);
  }

  /** Post-armor self damage from an explosion-typed hit (crystals, TNT carts). */
  public static float mitigatedExplosionDamage(LivingEntity victim, float rawDamage) {
    return mitigatedDamage(victim, explosionSource(victim), rawDamage);
  }

  /**
   * Vanilla {@code actuallyHurt} mitigation without the side effects: armor/toughness via {@code
   * CombatRules} (client-safe, the ServerLevel enchant path is instanceof-guarded), then
   * resistance/protection. Pre-absorption, like 3arthh4ck's DamageUtil. Never wears armor:
   * prediction must not consume durability.
   */
  public static float mitigatedDamage(LivingEntity victim, DamageSource source, float rawDamage) {
    float damage = rawDamage;
    if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
      damage =
          CombatRules.getDamageAfterAbsorb(
              victim,
              damage,
              source,
              victim.getArmorValue(),
              (float) victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }
    return magicAbsorb(victim, source, damage);
  }

  /** Vanilla-style check: a clear ray to the feet or mid-body with COLLIDER blocks. */
  public static boolean hasLineOfSight(Level level, Vec3 from, LivingEntity victim) {
    for (double scale : new double[] {0.0, 0.5}) {
      Vec3 to = new Vec3(victim.getX(), victim.getY(scale), victim.getZ());
      var hit =
          level.clip(
              new ClipContext(
                  from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, victim));
      if (hit.getType() == HitResult.Type.MISS) return true;
    }
    return false;
  }

  /**
   * Byte-for-byte port of {@code ServerExplosion.getSeenPercent}: per-axis grid steps {@code
   * 1/(size*2+1)}, half-texel insets on X/Z only (no Y inset), rays cast from each box sample point
   * to the blast center with COLLIDER blocks and the victim entity context.
   */
  private static double exposure(Level level, Vec3 center, LivingEntity victim, AABB box) {
    double stepX = 1.0 / (box.getXsize() * 2.0 + 1.0);
    double stepY = 1.0 / (box.getYsize() * 2.0 + 1.0);
    double stepZ = 1.0 / (box.getZsize() * 2.0 + 1.0);
    double offX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
    double offZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
    if (stepX < 0.0 || stepY < 0.0 || stepZ < 0.0) return 0.0;
    int seen = 0;
    int total = 0;
    for (double t = 0.0; t <= 1.0; t += stepX) {
      for (double u = 0.0; u <= 1.0; u += stepY) {
        for (double v = 0.0; v <= 1.0; v += stepZ) {
          Vec3 point =
              new Vec3(
                  Mth.lerp(t, box.minX, box.maxX) + offX,
                  Mth.lerp(u, box.minY, box.maxY),
                  Mth.lerp(v, box.minZ, box.maxZ) + offZ);
          var hit =
              level.clip(
                  new ClipContext(
                      point, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, victim));
          if (hit.getType() == HitResult.Type.MISS) seen++;
          total++;
        }
      }
    }
    return total == 0 ? 0.0 : (double) seen / total;
  }
}
