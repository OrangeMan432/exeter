package me.friendly.exeter.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

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
    float radius = power * 2.0F;
    double dist = victim.position().distanceTo(center) / radius;
    if (dist > 1.0) return 0.0F;
    double exposure = exposure(level, center, victim.getBoundingBox());
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

  /** Fraction of AABB grid points with a clear ray from the blast center (vanilla exposure). */
  private static double exposure(Level level, Vec3 source, AABB box) {
    int steps = 2;
    int total = 0;
    int clear = 0;
    for (int xi = 0; xi <= steps; xi++) {
      for (int yi = 0; yi <= steps; yi++) {
        for (int zi = 0; zi <= steps; zi++) {
          Vec3 point =
              new Vec3(
                  box.minX + box.getXsize() * xi / steps,
                  box.minY + box.getYsize() * yi / steps,
                  box.minZ + box.getZsize() * zi / steps);
          var hit =
              level.clip(
                  new ClipContext(
                      source,
                      point,
                      ClipContext.Block.COLLIDER,
                      ClipContext.Fluid.NONE,
                      CollisionContext.empty()));
          if (hit.getType() == HitResult.Type.MISS) clear++;
          total++;
        }
      }
    }
    return total == 0 ? 0.0 : (double) clear / total;
  }
}
