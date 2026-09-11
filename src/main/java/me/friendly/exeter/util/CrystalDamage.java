package me.friendly.exeter.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * BlackOut-ported explosion damage pipeline: exposure sampling, armor,
 * toughness, resistance, protection. Difficulty assumes HARD (anarchy).
 */
public final class CrystalDamage {

  private CrystalDamage() {}

  public static double crystalDamage(LivingEntity entity, Vec3 crystalPos) {
    return explosionDamage(entity, entity.getBoundingBox(), crystalPos, 6.0);
  }

  public static double explosionDamage(
      LivingEntity entity, AABB box, Vec3 pos, double strength) {
    if (entity == null || !entity.isAlive()) return 0.0;
    double q = strength * 2.0;
    Vec3 feet = new Vec3(box.minX, box.minY, box.minZ);
    double dist = feet.distanceTo(pos) / q;
    if (dist > 1.0) return 0.0;
    double exposure = getExposure(pos, box);
    double impact = (1.0 - dist) * exposure;
    double damage = (int) ((impact * impact + impact) * 3.5 * q + 1.0);
    damage = damage * 1.5; // HARD difficulty, standard on anarchy.
    damage = applyArmor(entity, damage);
    damage = applyResistance(entity, damage);
    return applyProtection(entity, damage);
  }

  public static double applyArmor(LivingEntity entity, double damage) {
    double armor = entity.getArmorValue();
    double toughness = entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
    double f = 2.0 + toughness / 4.0;
    return damage * (1.0 - Mth.clamp(armor - damage / f, armor * 0.2, 20.0) / 25.0);
  }

  public static double applyResistance(LivingEntity entity, double damage) {
    int amplifier =
        entity.hasEffect(MobEffects.RESISTANCE)
            ? entity.getEffect(MobEffects.RESISTANCE).getAmplifier()
            : 0;
    int j = 25 - (amplifier + 1) * 5;
    return Math.max(damage * j / 25.0, 0.0);
  }

  public static double applyProtection(LivingEntity entity, double damage) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return damage;
    Holder<Enchantment> prot = resolve(mc.level, Enchantments.PROTECTION);
    Holder<Enchantment> blast = resolve(mc.level, Enchantments.BLAST_PROTECTION);
    int total = 0;
    // 26.2 has no armor iterable: read the four slots directly.
    ItemStack[] armor = {
      entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),
      entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),
      entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS),
      entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET)
    };
    for (ItemStack stack : armor) {
      if (stack.isEmpty()) continue;
      if (prot != null) total += stack.getEnchantments().getLevel(prot);
      if (blast != null) total += stack.getEnchantments().getLevel(blast) * 2;
    }
    if (total > 0) {
      damage *= 1.0 - Mth.clamp(total, 0.0, 20.0) / 25.0;
    }
    return damage;
  }

  public static double getExposure(Vec3 source, AABB box) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null) return 0.0;
    double lx = box.getXsize();
    double ly = box.getYsize();
    double lz = box.getZsize();
    double deltaX = 1.0 / (lx * 2.0 + 1.0);
    double deltaY = 1.0 / (ly * 2.0 + 1.0);
    double deltaZ = 1.0 / (lz * 2.0 + 1.0);
    double offsetX = (1.0 - Math.floor(1.0 / deltaX) * deltaX) / 2.0;
    double offsetZ = (1.0 - Math.floor(1.0 / deltaZ) * deltaZ) / 2.0;
    double stepX = deltaX * lx;
    double stepY = deltaY * ly;
    double stepZ = deltaZ * lz;
    if (stepX < 0.0 || stepY < 0.0 || stepZ < 0.0) return 0.0;
    int hits = 0;
    int total = 0;
    for (double x = box.minX + offsetX; x <= box.maxX + offsetX; x += stepX) {
      for (double y = box.minY; y <= box.maxY; y += stepY) {
        for (double z = box.minZ + offsetZ; z <= box.maxZ + offsetZ; z += stepZ) {
          ClipContext ctx =
              new ClipContext(
                  source,
                  new Vec3(x, y, z),
                  ClipContext.Block.COLLIDER,
                  ClipContext.Fluid.NONE,
                  mc.player);
          if (mc.level.clip(ctx).getType() == HitResult.Type.MISS) {
            hits++;
          }
          total++;
        }
      }
    }
    return total == 0 ? 0.0 : (double) hits / total;
  }

  private static Holder<Enchantment> resolve(
      Level level, net.minecraft.resources.ResourceKey<Enchantment> key) {
    try {
      return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    } catch (Exception e) {
      return null;
    }
  }
}
